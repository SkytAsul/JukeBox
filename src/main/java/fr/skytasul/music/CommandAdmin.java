package fr.skytasul.music;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.nio.file.Files;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder;

import fr.skytasul.music.utils.Lang;
import fr.skytasul.music.utils.Playlists;

public class CommandAdmin implements CommandExecutor{

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		/*if (!(sender instanceof Player)){
			sender.sendMessage(Lang.PLAYER);
			return false;
		}
		Player p = (Player) sender;*/
		
		if (args.length == 0){
			sender.sendMessage(Lang.INCORRECT_SYNTAX);
			return false;
		}
		
		switch (args[0]){
		
		case "reload":
			sender.sendMessage(Lang.RELOAD_LAUNCH);
			try{
				JukeBox.getInstance().disableAll();
				JukeBox.getInstance().initAll();
			}catch (Exception ex){
				sender.sendMessage("§cError while reloading. Please check the console and send the stacktrace to SkytAsul on SpigotMC.");
				ex.printStackTrace();
			}
			sender.sendMessage(Lang.RELOAD_FINISH);
			break;
			
		case "player":
			if (args.length < 2){
				sender.sendMessage(Lang.INCORRECT_SYNTAX);
				return false;
			}
			OfflinePlayer pp = Bukkit.getOfflinePlayer(args[1]);
			if (pp == null){
				sender.sendMessage("§cUnknown player.");
				return false;
			}
			PlayerData pdata = PlayerData.players.get(pp.getUniqueId());
			String s = Lang.MUSIC_PLAYING + " ";
			if (pdata == null){
				s = s + "§cx";
			}else {
				if (pdata.songPlayer == null){
					s = s + "§cx";
				}else {
					Song song = pdata.songPlayer.getSong();
					s = JukeBox.getSongName(song);
				}
			}
			sender.sendMessage(s);
			break;
			
		case "play":
			if (args.length < 3){
				sender.sendMessage(Lang.INCORRECT_SYNTAX);
				return false;
			}
			if (args[1].equals("@a")){
				for (Player p : Bukkit.getOnlinePlayers()){
					args[1] = p.getName();
					String msg = play(args);
					if (!msg.isEmpty()) sender.sendMessage(p.getName() + " : " + msg);
				}
			}else {
				String msg = play(args);
				if (!msg.isEmpty()) sender.sendMessage(msg);
			}
			break;
			
		case "stop":
			if (args.length < 2){
				sender.sendMessage(Lang.INCORRECT_SYNTAX);
				return false;
			}
			if (args[1].equals("@a")){
				for (Player p : Bukkit.getOnlinePlayers()){
					sender.sendMessage(stop(p));
				}
			}else {
				Player cp = Bukkit.getPlayer(args[1]);
				if (cp == null){
					sender.sendMessage("§cUnknown player.");
					return false;
				}
				sender.sendMessage(stop(cp));
			}
			break;
			
		case "toggle":
			if (args.length < 2) {
				sender.sendMessage(Lang.INCORRECT_SYNTAX);
				return false;
			}
			if (args[1].equals("@a")) {
				for (Player p : Bukkit.getOnlinePlayers()) {
					toggle(p);
				}
			}else {
				Player cp = Bukkit.getPlayer(args[1]);
				if (cp == null) {
					sender.sendMessage("§cUnknown player.");
					return false;
				}
				toggle(cp);
			}
			break;
		
		case "setitem":
			if (!(sender instanceof Player)){
				sender.sendMessage("§cYou have to be a player to do that.");
				return false;
			}
			ItemStack is = ((Player) sender).getInventory().getItemInHand();
			if (is == null || is.getType() == Material.AIR){
				JukeBox.getInstance().jukeboxItem = null;
			}else JukeBox.getInstance().jukeboxItem = is;
			sender.sendMessage("§aItem edited. Now : §2" + ((JukeBox.getInstance().jukeboxItem == null) ? "null" : JukeBox.getInstance().jukeboxItem.toString()));
			break;
			
		case "download":
			if (args.length < 3){
				sender.sendMessage(Lang.INCORRECT_SYNTAX);
				return false;
			}
			try {
				File file = new File(JukeBox.songsFolder, args[2] + ".nbs");
				Files.copy(new URL(args[1]).openStream(), file.toPath());
				boolean valid = true;
				FileInputStream stream = new FileInputStream(file);
				try{
					Song song = NBSDecoder.parse(stream);
					if (song == null) valid = false;
				}catch (Throwable e){
					valid = false;
				}finally {
					stream.close();
					if (!valid) sender.sendMessage("§cDownloaded file is not a nbs song file.");
				}
				if (valid){
					sender.sendMessage("§aSong downloaded. To add it to the list, you must reload the plugin. (§o/amusic reload§r§a)");
				}else file.delete();
			} catch (Throwable e) {
				sender.sendMessage("§cError when downloading file.");
				e.printStackTrace();
			}
			break;
			
		case "shuffle":
			if (args.length < 2){
				sender.sendMessage(Lang.INCORRECT_SYNTAX);
				return false;
			}
			if (args[1].equals("@a")){
				for (Player p : Bukkit.getOnlinePlayers()){
					sender.sendMessage(p.getName() + " : " + shuffle(p));
				}
			}else {
				Player cp = Bukkit.getPlayer(args[1]);
				if (cp == null){
					sender.sendMessage("§cUnknown player.");
					return false;
				}
				sender.sendMessage(shuffle(cp));
			}
			break;
			
		case "particles":
			if (args.length < 2){
				sender.sendMessage(Lang.INCORRECT_SYNTAX);
				return false;
			}
			if (args[1].equals("@a")){
				for (Player p : Bukkit.getOnlinePlayers()){
					sender.sendMessage(p.getName() + " : " + particles(p));
				}
			}else {
				Player cp = Bukkit.getPlayer(args[1]);
				if (cp == null){
					sender.sendMessage("§cUnknown player.");
					return false;
				}
				sender.sendMessage(particles(cp));
			}
			break;
			
		case "join":
			if (args.length < 2){
				sender.sendMessage(Lang.INCORRECT_SYNTAX);
				return false;
			}
			if (args[1].equals("@a")){
				for (Player p : Bukkit.getOnlinePlayers()){
					sender.sendMessage(p.getName() + " : " + join(p));
				}
			}else {
				Player cp = Bukkit.getPlayer(args[1]);
				if (cp == null){
					sender.sendMessage("§cUnknown player.");
					return false;
				}
				sender.sendMessage(join(cp));
			}
			break;
			
		case "random":
			if (args.length < 2){
				sender.sendMessage(Lang.INCORRECT_SYNTAX);
				return false;
			}
			if (args[1].equals("@a")){
				for (Player p : Bukkit.getOnlinePlayers()){
					sender.sendMessage(p.getName() + " : " + random(p));
				}
			}else {
				Player cp = Bukkit.getPlayer(args[1]);
				if (cp == null){
					sender.sendMessage("§cUnknown player.");
					return false;
				}
				sender.sendMessage(random(cp));
			}
			break;
			
		case "volume":
			if (args.length < 3){
				sender.sendMessage(Lang.INCORRECT_SYNTAX);
				return false;
			}
			Player cp = Bukkit.getPlayer(args[1]);
			if (cp == null){
				sender.sendMessage("§cUnknown player.");
				return false;
			}
			pdata = PlayerData.players.get(cp.getUniqueId());
			try{
				int volume;
				if (args[2].equals("+")) {
					volume = pdata.getVolume() + 10;
				}else if (args[2].equals("-")) {
					volume = pdata.getVolume() - 10;
				}else volume = Integer.parseInt(args[2]);
				pdata.setVolume(volume);
				sender.sendMessage("§aVolume : " + pdata.getVolume());
			}catch (NumberFormatException ex){
				sender.sendMessage(Lang.INVALID_NUMBER);
			}
			break;
			
		case "loop":
			if (args.length < 2){
				sender.sendMessage(Lang.INCORRECT_SYNTAX);
				return false;
			}
			if (args[1].equals("@a")){
				for (Player p : Bukkit.getOnlinePlayers()){
					sender.sendMessage(p.getName() + " : " + loop(p));
				}
			}else {
				cp = Bukkit.getPlayer(args[1]);
				if (cp == null){
					sender.sendMessage("§cUnknown player.");
					return false;
				}
				sender.sendMessage(loop(cp));
			}
			break;
			
		case "next":
			if (args.length < 2) {
				sender.sendMessage(Lang.INCORRECT_SYNTAX);
				return false;
			}
			if (args[1].equals("@a")) {
				int i = 0;
				for (Player p : Bukkit.getOnlinePlayers()) {
					PlayerData.players.get(p.getUniqueId()).nextSong();
					i++;
				}
				sender.sendMessage("§aNext song for " + i + "players.");
			}else {
				cp = Bukkit.getPlayer(args[1]);
				if (cp == null) {
					sender.sendMessage("§cUnknown player.");
					return false;
				}
				PlayerData.players.get(cp.getUniqueId()).nextSong();
				sender.sendMessage("§aNext song for " + cp.getName());
			}
			break;
			
		default:
			sender.sendMessage(Lang.AVAILABLE_COMMANDS + " <reload|player|play|stop|toggle|setitem|download|shuffle|particles|join|random|volume|loop|next> ...");
			break;
		
		}
		
		return false;
	}
	
	private String play(String[] args){
		Player cp = Bukkit.getPlayer(args[1]);
		if (cp == null) return "§cUnknown player.";
		if (JukeBox.worlds && !JukeBox.worldsEnabled.contains(cp.getWorld().getName())) return "§cMusic isn't enabled in the world the player is into.";
		Song song;
		try{
			int id = Integer.parseInt(args[2]);
			try{
				song = JukeBox.getSongs().get(id);
			}catch (IndexOutOfBoundsException ex){
				return "§cError on §l" + id + " §r§c(inexistant)";
			}
		}catch (NumberFormatException ex){
			String fileName = args[2];
			for (int i = 3; i < args.length; i++){
				fileName = fileName + args[i] + (i == args.length-1 ? "" : " ");
			}
			song = JukeBox.getSongByFile(fileName);
			if (song == null) return Lang.INVALID_NUMBER;
		}
		PlayerData pdata = PlayerData.players.get(cp.getUniqueId());
		pdata.setPlaylist(Playlists.PLAYLIST, false);
		pdata.playSong(song);
		pdata.songPlayer.adminPlayed = true;
		return "";
	}
	
	private String stop(Player cp){
		PlayerData pdata = PlayerData.players.get(cp.getUniqueId());
		pdata.stopPlaying(true);
		return Lang.PLAYER_MUSIC_STOPPED + cp.getName();
	}
	
	private void toggle(Player cp){
		PlayerData pdata = PlayerData.players.get(cp.getUniqueId());
		pdata.togglePlaying();
	}
	
	private String shuffle(Player cp){
		PlayerData pdata = PlayerData.players.get(cp.getUniqueId());
		return "§aShuffle: " + pdata.setShuffle(!pdata.isShuffle());
	}
	
	private String join(Player cp){
		PlayerData pdata = PlayerData.players.get(cp.getUniqueId());
		return "§aJoin: " + pdata.setJoinMusic(!pdata.hasJoinMusic());
	}
	
	private String particles(Player cp){
		PlayerData pdata = PlayerData.players.get(cp.getUniqueId());
		return "§aParticles: " + pdata.setParticles(!pdata.hasParticles());
	}
	
	private String loop(Player cp){
		PlayerData pdata = PlayerData.players.get(cp.getUniqueId());
		return "§aLoop: " + pdata.setRepeat(!pdata.isRepeatEnabled());
	}
	
	private String random(Player cp){
		PlayerData pdata = PlayerData.players.get(cp.getUniqueId());
		Song song = pdata.playRandom();
		if (song == null) return "§aShuffle: §cnothing to play";
		return "§aShuffle: " + song.getTitle();
	}

}
