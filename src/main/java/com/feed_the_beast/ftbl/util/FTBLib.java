package com.feed_the_beast.ftbl.util;

import com.feed_the_beast.ftbl.FTBLibEventHandler;
import com.feed_the_beast.ftbl.FTBLibLang;
import com.feed_the_beast.ftbl.FTBLibMod;
import com.feed_the_beast.ftbl.api.ForgeWorldMP;
import com.feed_the_beast.ftbl.api.PackModes;
import com.feed_the_beast.ftbl.api.ServerTickCallback;
import com.feed_the_beast.ftbl.api.block.IBlockWithItem;
import com.feed_the_beast.ftbl.api.config.ConfigRegistry;
import com.feed_the_beast.ftbl.api.config.EnumNameMap;
import com.feed_the_beast.ftbl.api.events.ReloadEvent;
import com.feed_the_beast.ftbl.net.MessageReload;
import com.google.gson.JsonElement;
import com.latmod.lib.BroadcastSender;
import com.latmod.lib.io.LMConnection;
import com.latmod.lib.io.RequestMethod;
import com.latmod.lib.util.LMUtils;
import com.mojang.authlib.GameProfile;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemBlock;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.registry.IForgeRegistryEntry;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class FTBLib
{
    public static final boolean DEV_ENV = (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
    public static final Logger dev_logger = LogManager.getLogger("FTBLibDev");
    public static final String FORMATTING = "\u00a7";
    public static final Pattern textFormattingPattern = Pattern.compile("(?i)" + FORMATTING + "[0-9A-FK-OR]");

    public static final Comparator<ResourceLocation> RESOURCE_LOCATION_COMPARATOR = (o1, o2) ->
    {
        int i = o1.getResourceDomain().compareTo(o2.getResourceDomain());

        if(i == 0)
        {
            i = o1.getResourcePath().compareToIgnoreCase(o2.getResourcePath());
        }

        return i;
    };

    public static final EnumNameMap<EnumDyeColor> DYE_COLORS = new EnumNameMap<>(false, EnumDyeColor.values());
    public static final EnumNameMap<EnumFacing> FACINGS = new EnumNameMap<>(false, EnumFacing.VALUES);

    private static final Map<String, UUID> cachedUUIDs = new HashMap<>();
    public static boolean userIsLatvianModder = false;
    public static File folderConfig, folderMinecraft, folderModpack, folderLocal, folderWorld;

    public static void init(File configFolder)
    {
        folderConfig = configFolder;
        folderMinecraft = folderConfig.getParentFile();
        folderModpack = new File(folderMinecraft, "modpack/");
        folderLocal = new File(folderMinecraft, "local/");

        if(!folderModpack.exists())
        {
            folderModpack.mkdirs();
        }
        if(!folderLocal.exists())
        {
            folderLocal.mkdirs();
        }

        if(dev_logger instanceof org.apache.logging.log4j.core.Logger)
        {
            if(DEV_ENV)
            {
                ((org.apache.logging.log4j.core.Logger) dev_logger).setLevel(org.apache.logging.log4j.Level.ALL);
            }
            else
            {
                ((org.apache.logging.log4j.core.Logger) dev_logger).setLevel(org.apache.logging.log4j.Level.OFF);
            }
        }
        else
        {
            FTBLibMod.logger.info("DevLogger isn't org.apache.logging.log4j.core.Logger! It's " + dev_logger.getClass().getName());
        }
    }

    public static void reload(ICommandSender sender, ReloadType type, boolean login)
    {
        if(ForgeWorldMP.inst == null)
        {
            return;
        }

        long ms = System.currentTimeMillis();

        if(type.reload(Side.SERVER))
        {
            ConfigRegistry.reload();
            PackModes.reload();
            MinecraftForge.EVENT_BUS.post(new ReloadEvent(ForgeWorldMP.inst, sender, type, login));
        }

        if(!login)
        {
            if(hasOnlinePlayers())
            {
                for(EntityPlayerMP ep : getServer().getPlayerList().getPlayerList())
                {
                    new MessageReload(type, ForgeWorldMP.inst.getPlayer(ep), login).sendTo(ep);
                }
            }

            if(type.reload(Side.SERVER))
            {
                FTBLibLang.reload_server.printChat(BroadcastSender.INSTANCE, (System.currentTimeMillis() - ms) + "ms");
            }
        }
    }

    @Nonnull
    public static <K extends IForgeRegistryEntry<?>> K register(@Nonnull ResourceLocation id, @Nonnull K object)
    {
        object.setRegistryName(id);
        GameRegistry.register(object);

        if(object instanceof IBlockWithItem)
        {
            ItemBlock ib = ((IBlockWithItem) object).createItemBlock();
            ib.setRegistryName(id);
            GameRegistry.register(ib);
        }

        return object;
    }

    public static void addTile(Class<? extends TileEntity> c, ResourceLocation id)
    {
        if(c != null && id != null)
        {
            GameRegistry.registerTileEntity(c, id.toString().replace(':', '.'));
        }
    }

    public static void addEntity(Class<? extends Entity> c, String s, int id, Object mod)
    {
        EntityRegistry.registerModEntity(c, s, id, mod, 50, 1, true);
    }

    public static void addWorldGenerator(IWorldGenerator i, int w)
    {
        GameRegistry.registerWorldGenerator(i, w);
    }

    public static Fluid addFluid(Fluid f)
    {
        Fluid f1 = FluidRegistry.getFluid(f.getName());
        if(f1 != null)
        {
            return f1;
        }
        FluidRegistry.registerFluid(f);
        return f;
    }

    public static ITextComponent getChatComponent(Object o)
    {
        return (o instanceof ITextComponent) ? (ITextComponent) o : new TextComponentString(String.valueOf(o));
    }

    /**
     * Prints message to chat (doesn't translate it)
     */
    public static void printChat(ICommandSender ep, Object o)
    {
        if(ep == null)
        {
            ep = FTBLibMod.proxy.getClientPlayer();
        }
        if(ep != null)
        {
            ep.addChatMessage(getChatComponent(o));
        }
        else
        {
            FTBLibMod.logger.info(o);
        }
    }

    public static MinecraftServer getServer()
    {
        return FMLCommonHandler.instance().getMinecraftServerInstance();
    }

    public static Side getEffectiveSide()
    {
        return FMLCommonHandler.instance().getEffectiveSide();
    }

    public static boolean isDedicatedServer()
    {
        MinecraftServer mcs = getServer();
        return mcs != null && mcs.isDedicatedServer();
    }

    public static boolean hasOnlinePlayers()
    {
        return getServer() != null && !getServer().getPlayerList().getPlayerList().isEmpty();
    }

    public static String removeFormatting(String s)
    {
        if(s == null)
        {
            return null;
        }
        if(s.isEmpty())
        {
            return "";
        }
        return textFormattingPattern.matcher(s).replaceAll("");
    }

    public static WorldServer getServerWorld()
    {
        MinecraftServer ms = getServer();
        if(ms == null || ms.worldServers.length < 1)
        {
            return null;
        }
        return ms.worldServers[0];
    }

    public static int runCommand(ICommandSender ics, String s) throws CommandException
    {
        return getServer().getCommandManager().executeCommand(ics, s);
    }

    public static int runCommand(ICommandSender ics, String cmd, String[] args) throws CommandException
    {
        StringBuilder sb = new StringBuilder();
        sb.append(cmd);
        if(args != null && args.length > 0)
        {
            for(String arg : args)
            {
                sb.append(' ');
                sb.append(arg);
            }
        }

        return runCommand(ics, sb.toString());
    }

    public static void addCallback(ServerTickCallback c)
    {
        if(c.maxTick == 0)
        {
            c.onCallback();
        }
        else
        {
            FTBLibEventHandler.pendingCallbacks.add(c);
        }
    }

    public static boolean isOP(GameProfile p)
    {
        return !isDedicatedServer() || (p != null && p.getId() != null && getServerWorld() != null && getServer().getPlayerList().getOppedPlayers().getPermissionLevel(p) > 0);
    }

    public static Collection<ICommand> getAllCommands(MinecraftServer server, ICommandSender sender)
    {
        Collection<ICommand> commands = new HashSet<>();

        for(ICommand c : server.getCommandManager().getCommands().values())
        {
            if(c.checkPermission(server, sender))
            {
                commands.add(c);
            }
        }

        return commands;
    }

    public static UUID getPlayerID(String s)
    {
        if(s == null || s.isEmpty())
        {
            return null;
        }

        String key = s.trim().toLowerCase();

        if(!cachedUUIDs.containsKey(key))
        {
            cachedUUIDs.put(key, null);

            try
            {
                JsonElement e = new LMConnection(RequestMethod.GET, "https://api.mojang.com/users/profiles/minecraft/" + s).connect().asJson();
                cachedUUIDs.put(key, LMUtils.fromString(e.getAsJsonObject().get("id").getAsString()));
            }
            catch(Exception e)
            {
            }
        }

        return cachedUUIDs.get(key);
    }

    //null - can't, TRUE - always spawns, FALSE - only spawns at night
    public static Boolean canMobSpawn(World world, BlockPos pos)
    {
        if(world == null || pos == null || pos.getY() < 0 || pos.getY() >= 256)
        {
            return null;
        }
        Chunk chunk = world.getChunkFromBlockCoords(pos);

        if(!WorldEntitySpawner.canCreatureTypeSpawnAtLocation(EntityLiving.SpawnPlacementType.ON_GROUND, world, pos) || chunk.getLightFor(EnumSkyBlock.BLOCK, pos) >= 8)
        {
            return null;
        }

        AxisAlignedBB aabb = new AxisAlignedBB(pos.getX() + 0.2, pos.getY() + 0.01, pos.getZ() + 0.2, pos.getX() + 0.8, pos.getY() + 1.8, pos.getZ() + 0.8);
        if(!world.checkNoEntityCollision(aabb) || world.containsAnyLiquid(aabb))
        {
            return null;
        }

        if(chunk.getLightFor(EnumSkyBlock.SKY, pos) >= 8)
        {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    public static Entity getEntityByUUID(World worldObj, UUID uuid)
    {
        if(worldObj == null || uuid == null)
        {
            return null;
        }

        for(Entity e : worldObj.loadedEntityList)
        {
            if(e.getUniqueID().equals(uuid))
            {
                return e;
            }
        }

        return null;
    }

    public static void registerServerTickable(MinecraftServer server, ITickable tickable)
    {
        try
        {
            Field field = ReflectionHelper.findField(MinecraftServer.class, "tickables", "field_71322_p");
            field.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<ITickable> list = (List<ITickable>) field.get(server);

            list.add(tickable);
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
    }
}