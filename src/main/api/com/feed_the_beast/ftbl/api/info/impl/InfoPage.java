package com.feed_the_beast.ftbl.api.info.impl;

import com.feed_the_beast.ftbl.api.client.gui.widgets.ButtonLM;
import com.feed_the_beast.ftbl.api.info.IGuiInfoPage;
import com.feed_the_beast.ftbl.api.info.IGuiInfoPageTree;
import com.feed_the_beast.ftbl.api.info.IInfoPageTheme;
import com.feed_the_beast.ftbl.api.info.IInfoTextLine;
import com.feed_the_beast.ftbl.api.info.IResourceProvider;
import com.feed_the_beast.ftbl.gui.GuiInfo;
import com.feed_the_beast.ftbl.net.MessageDisplayInfo;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.latmod.lib.RemoveFilter;
import com.latmod.lib.json.LMJsonUtils;
import com.latmod.lib.util.LMMapUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InfoPage implements IGuiInfoPage // GuideFile
{
    private static final Comparator<Map.Entry<String, InfoPage>> COMPARATOR = (o1, o2) -> InfoPageHelper.getTitleComponent(o1.getValue(), o1.getKey()).getUnformattedText().compareToIgnoreCase(InfoPageHelper.getTitleComponent(o2.getValue(), o2.getKey()).getUnformattedText());
    private static final RemoveFilter<Map.Entry<String, InfoPage>> CLEANUP_FILTER = entry -> entry.getValue().childPages.isEmpty() && InfoPageHelper.getUnformattedText(entry.getValue()).trim().isEmpty();

    private final List<IInfoTextLine> text;
    private final LinkedHashMap<String, InfoPage> childPages;
    public InfoPage parent = null;
    public IInfoPageTheme theme;
    public IResourceProvider resourceProvider;
    private ITextComponent title;

    public InfoPage()
    {
        text = new ArrayList<>();
        childPages = new LinkedHashMap<>();
    }

    public InfoPage setTitle(@Nullable ITextComponent c)
    {
        title = c;
        return this;
    }

    public InfoPage setParent(InfoPage c)
    {
        parent = c;
        return this;
    }

    public void println(Object o)
    {
        if(o == null)
        {
            text.add(null);
        }
        else if(o instanceof IInfoTextLine)
        {
            text.add((IInfoTextLine) o);
        }
        else if(o instanceof ITextComponent)
        {
            ITextComponent c = (ITextComponent) o;

            if(c instanceof TextComponentString && c.getStyle().isEmpty() && c.getSiblings().isEmpty())
            {
                text.add(new InfoTextLineString(((TextComponentString) c).getText()));
            }
            else
            {
                text.add(new InfoExtendedTextLine(c));
            }
        }
        else
        {
            text.add(new InfoTextLineString(String.valueOf(o)));
        }
    }

    public void addSub(String id, InfoPage c)
    {
        childPages.put(id, c);
        c.setParent(this);
    }

    public InfoPage getSub(String id)
    {
        InfoPage c = childPages.get(id);
        if(c == null)
        {
            c = new InfoPage();
            c.setParent(this);
            childPages.put(id, c);
        }

        return c;
    }

    public void clear()
    {
        setTitle(null);
        text.clear();
        childPages.clear();
        theme = null;
    }

    public void cleanup()
    {
        childPages.values().forEach(InfoPage::cleanup);
        LMMapUtils.removeAll(childPages, CLEANUP_FILTER);
    }

    public void sortAll()
    {
        LMMapUtils.sortMap(childPages, COMPARATOR);

        for(InfoPage c : childPages.values())
        {
            c.sortAll();
        }
    }

    public void copyFrom(InfoPage c)
    {
        for(IInfoTextLine l : c.text)
        {
            text.add(l == null ? null : InfoPageHelper.createLine(this, l.getSerializableElement()));
        }

        for(Map.Entry<String, InfoPage> entry : c.childPages.entrySet())
        {
            InfoPage p1 = new InfoPage();
            p1.copyFrom(entry.getValue());
            addSub(entry.getKey(), p1);
        }
    }

    public InfoPage copy()
    {
        InfoPage page = new InfoPage();
        page.fromJson(getSerializableElement());
        return page;
    }

    public InfoPage getParentTop()
    {
        if(parent == null)
        {
            return this;
        }

        return parent.getParentTop();
    }

    @Nonnull
    @Override
    public JsonElement getSerializableElement()
    {
        JsonObject o = new JsonObject();

        if(getName() != null)
        {
            o.add("N", LMJsonUtils.serializeTextComponent(getName()));
        }

        if(!text.isEmpty())
        {
            JsonArray a = new JsonArray();
            for(IInfoTextLine c : text)
            {
                a.add(c == null ? JsonNull.INSTANCE : c.getSerializableElement());
            }
            o.add("T", a);
        }

        if(!childPages.isEmpty())
        {
            JsonObject o1 = new JsonObject();
            for(Map.Entry<String, InfoPage> entry : childPages.entrySet())
            {
                o1.add(entry.getKey(), entry.getValue().getSerializableElement());
            }
            o.add("S", o1);
        }

        if(theme != null)
        {
            o.add("C", theme.getSerializableElement());
        }

        return o;
    }

    @Override
    public void fromJson(@Nonnull JsonElement e)
    {
        clear();

        if(!e.isJsonObject())
        {
            return;
        }

        JsonObject o = e.getAsJsonObject();

        if(o.has("N"))
        {
            setTitle(LMJsonUtils.deserializeTextComponent(o.get("N")));
        }

        if(o.has("T"))
        {
            JsonArray a = o.get("T").getAsJsonArray();
            for(int i = 0; i < a.size(); i++)
            {
                text.add(InfoPageHelper.createLine(this, a.get(i)));
            }
        }

        if(o.has("S"))
        {
            JsonObject o1 = o.get("S").getAsJsonObject();

            for(Map.Entry<String, JsonElement> entry : o1.entrySet())
            {
                InfoPage c = new InfoPage();
                c.setParent(this);
                c.fromJson(entry.getValue());
                childPages.put(entry.getKey(), c);
            }
        }

        if(o.has("C"))
        {
            theme = new InfoPageTheme();
            theme.fromJson(o.get("C"));
        }
    }

    public MessageDisplayInfo displayGuide(EntityPlayerMP ep, String id)
    {
        MessageDisplayInfo m = new MessageDisplayInfo(id, this);
        if(ep != null && !(ep instanceof FakePlayer))
        {
            m.sendTo(ep);
        }
        return m;
    }

    @Nullable
    @Override
    public ITextComponent getName()
    {
        return title;
    }

    @Nonnull
    @Override
    public final List<IInfoTextLine> getText()
    {
        return text;
    }

    @Nonnull
    @Override
    public final Map<String, ? extends IGuiInfoPage> getPages()
    {
        return childPages;
    }

    @Nonnull
    public final IInfoPageTheme getTheme()
    {
        return (theme == null) ? ((parent == null) ? InfoPageTheme.DEFAULT : parent.getTheme()) : theme;
    }

    @Nonnull
    public final IResourceProvider getResourceProvider()
    {
        return (resourceProvider == null) ? ((parent == null) ? URLResourceProvider.INSTANCE : parent.getResourceProvider()) : resourceProvider;
    }

    @SideOnly(Side.CLIENT)
    public void refreshGui(@Nonnull GuiInfo gui)
    {
    }

    @SideOnly(Side.CLIENT)
    public ButtonLM createSpecialButton(GuiInfo gui)
    {
        return null;
    }

    @SideOnly(Side.CLIENT)
    public ButtonInfoPage createButton(GuiInfo gui, IGuiInfoPageTree page)
    {
        return new ButtonInfoPage(gui, page, null);
    }
}