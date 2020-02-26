package fr.skytasul.music.utils;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.entity.Player;

public class Particles {

	private static Random ran = new Random();
	
	public static void sendParticles(Player p){
		p.spawnParticle(Particle.NOTE, p.getEyeLocation().add(p.getLocation().getDirection().multiply(2)), 1, ran.nextInt(24) / 24, 0.5, 0.1, 1, null);
	}

}
