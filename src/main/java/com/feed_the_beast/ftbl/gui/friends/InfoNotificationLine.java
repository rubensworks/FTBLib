package com.feed_the_beast.ftbl.gui.friends;

import com.feed_the_beast.ftbl.api.MouseButton;
import com.feed_the_beast.ftbl.api.client.gui.GuiIcons;
import com.feed_the_beast.ftbl.api.client.gui.GuiLM;
import com.feed_the_beast.ftbl.api.client.gui.GuiLang;
import com.feed_the_beast.ftbl.api.info.IGuiInfoPageTree;
import com.feed_the_beast.ftbl.api.info.impl.ButtonInfoTextLine;
import com.feed_the_beast.ftbl.api.info.impl.EmptyInfoPageLine;
import com.feed_the_beast.ftbl.api.notification.ClientNotifications;
import com.feed_the_beast.ftbl.gui.GuiInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Created by LatvianModder on 23.03.2016.
 */
@SideOnly(Side.CLIENT)
public class InfoNotificationLine extends EmptyInfoPageLine
{
    public class ButtonInfoNotification extends ButtonInfoTextLine
    {
        public ClientNotifications.NotificationWidget widget;

        public ButtonInfoNotification(GuiInfo g)
        {
            super(g, null);
            widget = new ClientNotifications.NotificationWidget(notification.notification);
            height = widget.height;
        }

        @Override
        public void addMouseOverText(@Nonnull GuiLM gui, @Nonnull List<String> l)
        {
            if(gui.mouseX >= getAX() + width - 32)
            {
                l.add(GuiLang.button_close.translate());
            }
        }

        @Override
        public void onClicked(@Nonnull GuiLM gui, @Nonnull MouseButton button)
        {
            GuiLM.playClickSound();

            if(gui.mouseX < getAX() + width - 32 && notification.notification.getClickAction() != null)
            {
                notification.notification.getClickAction().onClicked(button);
            }

            ClientNotifications.Perm.map.remove(widget.notification.ID);
            gui.refreshWidgets();
        }

        @Override
        public void renderWidget(@Nonnull GuiLM gui)
        {
            widget.width = width = ((GuiInfo) gui).panelText.width - 4;

            double ay = getAY();
            double ax = getAX();

            widget.render(gui.mc, ax, ay);

            if(gui.isMouseOver(this))
            {
                GlStateManager.color(1F, 1F, 1F, 0.2F);
                GuiLM.drawBlankRect(ax, ay, width, height);

                if(gui.mouseX >= ax + width - 32)
                {
                    GuiLM.drawBlankRect(ax + width - 32, ay, 32, height);
                    GlStateManager.color(1F, 1F, 1F, 1F);
                }

                GuiLM.render(GuiIcons.close, ax + width - 32, ay + (height - 32D) / 2D, 32D, 32D);
            }
        }
    }

    public final ClientNotifications.Perm notification;

    public InfoNotificationLine(ClientNotifications.Perm p)
    {
        notification = p;
    }

    @Override
    @Nonnull
    @SideOnly(Side.CLIENT)
    public ButtonInfoTextLine createWidget(GuiInfo gui, IGuiInfoPageTree page)
    {
        return new ButtonInfoNotification(gui);
    }
}
