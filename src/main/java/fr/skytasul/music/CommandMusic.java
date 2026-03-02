package fr.skytasul.music;

import fr.skytasul.music.forms.BedrockMusicForm;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.skytasul.music.utils.BedrockChecker;
import fr.skytasul.music.utils.Lang;

public class CommandMusic implements CommandExecutor{

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if (!(sender instanceof Player)){
			sender.sendMessage(Lang.PLAYER);
			return false;
		}

		Player p = (Player) sender;

		// Use Floodgate detection from JukeBox class
		if (JukeBox.isFloodgateAvailable() && BedrockChecker.isBedrockPlayer(p)) {
			// Display form interface for Bedrock Edition players
			openBedrockForm(p);
		} else {
			// Display chest menu for Java Edition players
			open(p);
		}

		return false;
	}

	/**
	 * Open form interface for Bedrock Edition players
	 */
	private void openBedrockForm(Player p) {
		if (JukeBox.worlds && !JukeBox.worldsEnabled.contains(p.getWorld().getName())) return;
		PlayerData pdata = JukeBox.getInstance().datas.getDatas(p);
		if (pdata == null) {
			p.sendMessage("§cLoading player... Try again! ");
			return;
		}

		// Create and display Bedrock Edition form
		BedrockMusicForm form = new BedrockMusicForm(p, pdata);
		form.showMainMenu();
	}
	
	public static void open(Player p){
		if (JukeBox.worlds && !JukeBox.worldsEnabled.contains(p.getWorld().getName())) return;
		PlayerData pdata = JukeBox.getInstance().datas.getDatas(p);
		if (pdata == null) {
			p.sendMessage("§cLoading player... Try again!");
			return;
		}
		if (pdata.linked != null){
			JukeBoxInventory inv = pdata.linked;
			inv.setSongsPage(p);
			inv.openInventory(p);
		}else new JukeBoxInventory(p, pdata);
	}

}
