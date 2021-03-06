package com.feed_the_beast.ftbl.api.block;

import com.feed_the_beast.ftbl.api.item.IMaterial;
import com.feed_the_beast.ftbl.util.FTBLib;
import com.latmod.lib.math.BlockStateSerializer;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Created by LatvianModder on 09.08.2016.
 */
public abstract class BlockWithVariants<T extends Enum<T> & BlockWithVariants.IBlockVariant> extends BlockLM
{
    public interface IBlockVariant extends IMaterial
    {
        @Nullable
        default Class<? extends TileEntity> getTileEntityClass()
        {
            return null;
        }

        @Nullable
        default TileEntity createTileEntity(World world)
        {
            return null;
        }

        @Nonnull
        default BlockRenderLayer getLayer()
        {
            return BlockRenderLayer.SOLID;
        }

        @Nonnull
        default MapColor getMapColor()
        {
            return MapColor.STONE;
        }

        default boolean isOpaqueCube()
        {
            return getLayer() == BlockRenderLayer.SOLID;
        }

        @Nonnull
        Material getMaterial();
    }

    private BlockVariantLookup<T> metaLookup;

    public BlockWithVariants(Material m, CreativeTabs tab)
    {
        super(m);
        setCreativeTab(tab);
    }

    @Nonnull
    public abstract BlockVariantLookup<T> createMetaLookup();

    @Nonnull
    public BlockVariantLookup<T> getMetaLookup()
    {
        if(metaLookup == null)
        {
            metaLookup = createMetaLookup();
        }

        return metaLookup;
    }

    @Override
    public ItemBlock createItemBlock()
    {
        return new ItemBlockLM(this)
        {
            @Override
            @Nullable
            public String getVariantName(int meta)
            {
                return getMetaLookup().get(meta).getName();
            }
        };
    }

    @Nonnull
    @Override
    @Deprecated
    public Material getMaterial(IBlockState state)
    {
        return state.getValue(getMetaLookup().getProperty()).getMaterial();
    }

    @Override
    public boolean hasTileEntity(IBlockState state)
    {
        return state.getValue(getMetaLookup().getProperty()).getTileEntityClass() != null;
    }

    @Nonnull
    @Override
    public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state)
    {
        return state.getValue(getMetaLookup().getProperty()).createTileEntity(world);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubBlocks(@Nonnull Item itemIn, CreativeTabs tab, List<ItemStack> list)
    {
        for(T e : getMetaLookup().getValues())
        {
            list.add(e.getStack(1));
        }
    }

    @Override
    public int damageDropped(IBlockState state)
    {
        return state.getValue(getMetaLookup().getProperty()).getMetadata();
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState getStateFromMeta(int meta)
    {
        return getDefaultState().withProperty(getMetaLookup().getProperty(), getMetaLookup().get(meta));
    }

    @Nonnull
    @Override
    @Deprecated
    public MapColor getMapColor(IBlockState state)
    {
        return state.getValue(getMetaLookup().getProperty()).getMapColor();
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(getMetaLookup().getProperty()).getMetadata();
    }

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, getMetaLookup().getProperty());
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, @Nonnull BlockRenderLayer layer)
    {
        return layer == state.getValue(getMetaLookup().getProperty()).getLayer();
    }

    @Override
    @Deprecated
    public boolean isOpaqueCube(IBlockState state)
    {
        return state.getValue(getMetaLookup().getProperty()).isOpaqueCube();
    }

    // Helpers //

    public void registerTileEntities()
    {
        for(T e : getMetaLookup())
        {
            if(e.getTileEntityClass() != null)
            {
                FTBLib.addTile(e.getTileEntityClass(), new ResourceLocation(getRegistryName().getResourceDomain(), e.getName()));
            }
        }
    }

    public void registerModels()
    {
        Item item = Item.getItemFromBlock(this);

        for(T e : getMetaLookup())
        {
            ModelLoader.setCustomModelResourceLocation(item, e.getMetadata(), new ModelResourceLocation(getRegistryName(), BlockStateSerializer.getString(blockState.getBaseState().withProperty(getMetaLookup().getProperty(), e))));
        }
    }
}