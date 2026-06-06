package com.omnicore.command;

import com.omnicore.OmniCore;
import com.omnicore.module.Module;
import com.omnicore.module.movement.PathfindingModule;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

public class CommandManager {
    private static final String PREFIX = ".omni";

    public void registerCommands() {
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (!message.startsWith(PREFIX)) return true;
            handleCommand(message.substring(PREFIX.length()).trim());
            return false; // block the message from being sent
        });
    }

    private void handleCommand(String input) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        String[] args = input.split(" ");
        if (args.length == 0) return;

        switch (args[0].toLowerCase()) {
            case "help" -> sendMessage("§eCommands: toggle, list, goto <x> <z>, goto <x> <y> <z>, follow [name], stop, dig <on|off>, status, combat <enable|disable>");
            case "list" -> {
                StringBuilder sb = new StringBuilder("§eModules: ");
                OmniCore.getInstance().getModuleManager().getModules().forEach(m ->
                    sb.append(m.isEnabled() ? "§a" : "§c").append(m.getName()).append("§e, ")
                );
                sendMessage(sb.toString());
            }
            case "toggle" -> {
                if (args.length < 2) { sendMessage("§cUsage: .omni toggle <module>"); return; }
                String moduleName = args[1];
                Optional<Module> module = OmniCore.getInstance().getModuleManager().getModule(moduleName);
                module.ifPresentOrElse(m -> {
                    m.toggle();
                    sendMessage("§e" + m.getName() + " §" + (m.isEnabled() ? "a" : "c") + (m.isEnabled() ? "enabled" : "disabled"));
                }, () -> sendMessage("§cModule not found: " + moduleName));
            }
            case "combat" -> {
                if (args.length < 2) return;
                boolean enable = args[1].equalsIgnoreCase("enable");
                OmniCore.getInstance().getModuleManager().getModules().stream()
                    .filter(m -> m.getCategory().getName().equals("Combat"))
                    .forEach(m -> m.setEnabled(enable));
                sendMessage("§eCombat modules " + (enable ? "§aenabled" : "§cdisabled"));
            }
            case "goto" -> {
                // .omni goto <x> <z>  OR  .omni goto <x> <y> <z>
                try {
                    int x, y, z;
                    if (args.length == 4) {
                        x = Integer.parseInt(args[1]);
                        y = Integer.parseInt(args[2]);
                        z = Integer.parseInt(args[3]);
                    } else if (args.length == 3) {
                        x = Integer.parseInt(args[1]);
                        y = mc.player != null ? mc.player.getBlockY() : 64;
                        z = Integer.parseInt(args[2]);
                    } else {
                        sendMessage("§cUsage: .omni goto <x> <z>  OR  .omni goto <x> <y> <z>");
                        return;
                    }
                    OmniCore.getInstance().getModuleManager()
                        .getModule(PathfindingModule.class)
                        .ifPresent(m -> m.goTo(new BlockPos(x, y, z)));
                    sendMessage("§eNavigating to §a" + x + ", " + y + ", " + z);
                } catch (NumberFormatException e) {
                    sendMessage("§cInvalid coordinates.");
                }
            }
            case "follow" -> {
                // .omni follow           -> nearest entity
                // .omni follow <name>    -> player by name
                OmniCore.getInstance().getModuleManager()
                    .getModule(com.omnicore.module.movement.FollowModule.class)
                    .ifPresent(m -> {
                        if (args.length >= 2) m.followPlayer(args[1]);
                        else                 m.followNearest();
                    });
                sendMessage("§eFollowing " + (args.length >= 2 ? "§a" + args[1] : "§anearest entity"));
            }
            case "stop" -> {
                // Stop all movement
                OmniCore.getInstance().getModuleManager()
                    .getModule(PathfindingModule.class)
                    .ifPresent(m -> m.setEnabled(false));
                OmniCore.getInstance().getModuleManager()
                    .getModule(com.omnicore.module.movement.FollowModule.class)
                    .ifPresent(m -> m.setEnabled(false));
                OmniCore.getInstance().getModuleManager()
                    .getModule(com.omnicore.module.movement.AutoWalkModule.class)
                    .ifPresent(m -> m.setEnabled(false));
                sendMessage("§eAll movement §cstopped");
            }
            case "dig" -> {
                // .omni dig <on|off>
                if (args.length < 2) { sendMessage("§cUsage: .omni dig <on|off>"); return; }
                boolean digOn = args[1].equalsIgnoreCase("on");
                OmniCore.getInstance().getModuleManager()
                    .getModule(PathfindingModule.class)
                    .ifPresent(m -> m.setAllowDig(digOn));
                sendMessage("§eDig mode §" + (digOn ? "aON" : "cOFF"));
            }
            case "status" -> {
                // Show pathfinding status
                OmniCore.getInstance().getModuleManager()
                    .getModule(PathfindingModule.class)
                    .ifPresentOrElse(
                        m -> sendMessage("§ePathfinding: §a" + m.getStatus()),
                        () -> sendMessage("§cPathfinding module not found")
                    );
            }
            default -> sendMessage("§cUnknown command. Type .omni help for help.");
        }
    }

    private void sendMessage(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal(message), false);
        }
    }
}
