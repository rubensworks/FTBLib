package com.feed_the_beast.ftbl.cmd;

import com.feed_the_beast.ftbl.api.cmd.CmdEditConfigBase;
import com.feed_the_beast.ftbl.api.config.ConfigContainer;
import com.feed_the_beast.ftbl.api.config.ConfigRegistry;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

public class CmdEditConfig extends CmdEditConfigBase
{
    public CmdEditConfig()
    {
        super("edit_config");
    }

    @Override
    public ConfigContainer getConfigContainer(ICommandSender sender) throws CommandException
    {
        return ConfigRegistry.CONTAINER;
    }
}