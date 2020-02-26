package fr.skytasul.music;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.skytasul.music.utils.Lang;

public class CommandMusic implements CommandExecutor{

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if (!(sender instanceof Player)){
			sender.sendMessage(Lang.PLAYER);
			return false;
		}
		
		Player p = (Player) sender;
		open(p);
		
		return false;
	}
	
	public static void open(Player p){
		if (JukeBox.worlds && !JukeBox.worldsEnabled.contains(p.getWorld())) return;
		PlayerData pdata = PlayerData.players.get(p.getUniqueId());
		if (pdata.linked != null){
			JukeBoxInventory inv = pdata.linked;
			inv.setSongsPage();
			inv.openInventory(p);
		}else new JukeBoxInventory(p);
	}

}
