package com.mmyzd.jstweaker;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import com.mmyzd.jstweaker.scripting.ScriptManager;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

public class JSTCommand extends CommandBase {
	
	private static final String[] SUB_COMMANDS = {"help", "reload", "exec"};
	
	@Override
	public int getRequiredPermissionLevel() {
        return 2;
    }

	@Override
	public String getCommandName() {
		return "jst";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/jst <" + StringUtils.join(SUB_COMMANDS, "|") + ">";
	}

	@Override
	public List<String> getTabCompletionOptions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
		if (args.length != 1) return null;
		ArrayList<String> ret = new ArrayList<String>();
		for (int i = 0; i < SUB_COMMANDS.length; i++)
			if (SUB_COMMANDS[i].startsWith(args[0].toLowerCase())) {
				ret.add(SUB_COMMANDS[i]);
				return ret;
			}
		ret.add(SUB_COMMANDS[0]);
		return ret;
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length == 1 && args[0].toLowerCase().equals(SUB_COMMANDS[0])) {
			sender.addChatMessage(new TextComponentString("Commands for <JSTweaker> MOD:"));
			sender.addChatMessage(new TextComponentString("  /jst " + SUB_COMMANDS[0] + "  -  Get help"));
			sender.addChatMessage(new TextComponentString("  /jst " + SUB_COMMANDS[1] + "  -  Reload all scripts"));
			sender.addChatMessage(new TextComponentString("  /jst " + SUB_COMMANDS[2] + " <fileName>  -  Execute script"));
		} else if (args.length == 1 && args[0].toLowerCase().equals(SUB_COMMANDS[1])) {
			JSTLogger.get().clearErrorMessages();
			ScriptManager.getInstance().reload();
			ArrayList<String> messages = JSTLogger.get().getErrorMessages();
			for (String s: messages) {
				sender.addChatMessage(new TextComponentString(s));
			}
			if (messages.isEmpty()) {
				sender.addChatMessage(new TextComponentString("Scripts reloaded successfully."));
			} else {
				sender.addChatMessage(new TextComponentString("Scripts reloaded with errors. Check JSTweaker.log for details."));
			}
		} else if (args.length == 2 && args[0].toLowerCase().equals(SUB_COMMANDS[2])) {
			JSTLogger.get().clearErrorMessages();
			ScriptManager.getInstance().exec(args[1]);
			ArrayList<String> messages = JSTLogger.get().getErrorMessages();
			for (String s: messages) {
				sender.addChatMessage(new TextComponentString(s));
			}
			if (messages.isEmpty()) {
				sender.addChatMessage(new TextComponentString(args[1] + " executed successfully."));
			} else {
				sender.addChatMessage(new TextComponentString(args[1] + " executed with errors. Check JSTweaker.log for details."));
			}
		} else {
			sender.addChatMessage(new TextComponentString("Usage: " + getCommandUsage(sender)));
		}
	}

}
