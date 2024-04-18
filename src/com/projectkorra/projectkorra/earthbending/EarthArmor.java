package com.projectkorra.projectkorra.earthbending;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.projectkorra.projectkorra.region.RegionProtection;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;


import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.ElementalAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempArmor;
import com.projectkorra.projectkorra.util.TempBlock;

public class EarthArmor extends EarthAbility implements Listener {

	private static Map<String, Integer> COLORS = new HashMap<>();

	private boolean formed;
	private Material headMaterial;
	private Material legsMaterial;
	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	private long interval;
	@Attribute(Attribute.DURATION)
	private long maxDuration;
	@Attribute(Attribute.SELECT_RANGE)
	private double selectRange;
	private Block headBlock;
	private Block legsBlock;
	private Location headBlockLocation;
	private Location legsBlockLocation;
	private boolean active;
	private PotionEffect oldAbsorbtion = null;
	private double goldHearts;
	@Attribute("GoldHearts")
	private int maxGoldHearts;
	private TempArmor armor;

	public EarthArmor(final Player player) {
		super(player);
		if (hasAbility(player, EarthArmor.class) || !this.canBend()) {
			return;
		}

		this.formed = false;
		this.active = true;
		this.interval = 2000;
		this.goldHearts = 0;
		this.cooldown = getConfig().getLong("Abilities.Earth.EarthArmor.Cooldown");
		this.maxDuration = getConfig().getLong("Abilities.Earth.EarthArmor.MaxDuration");
		this.selectRange = getConfig().getDouble("Abilities.Earth.EarthArmor.SelectRange");
		this.maxGoldHearts = getConfig().getInt("Abilities.Earth.EarthArmor.GoldHearts");

		if (this.bPlayer.isAvatarState()) {
			this.cooldown = getConfig().getLong("Abilities.Avatar.AvatarState.Earth.EarthArmor.Cooldown");
			this.maxGoldHearts = getConfig().getInt("Abilities.Avatar.AvatarState.Earth.EarthArmor.GoldHearts");
		}

		if (COLORS.isEmpty()) defineColors();

		this.headBlock = this.getTargetEarthBlock((int) this.selectRange);
		if (!GeneralMethods.isRegionProtectedFromBuild(this, this.headBlock.getLocation()) && this.getEarthbendableBlocksLength(this.headBlock, new Vector(0, -1, 0), 2) >= 2) {
			this.legsBlock = this.headBlock.getRelative(BlockFace.DOWN);
			this.headMaterial = this.headBlock.getType();
			this.legsMaterial = this.legsBlock.getType();
			this.headBlockLocation = this.headBlock.getLocation();
			this.legsBlockLocation = this.legsBlock.getLocation();

			final Block oldHeadBlock = this.headBlock;
			final Block oldLegsBlock = this.legsBlock;

			if (!this.moveBlocks()) {
				return;
			}
			if ((TempBlock.isTempBlock(oldHeadBlock) && !isBendableEarthTempBlock(oldHeadBlock))
					|| (TempBlock.isTempBlock(oldLegsBlock) && !isBendableEarthTempBlock(oldLegsBlock))) {
				return;
			}
			if (isEarthRevertOn()) {
				addTempAirBlock(oldHeadBlock);
				addTempAirBlock(oldLegsBlock);
			} else {
				GeneralMethods.removeBlock(oldHeadBlock);
				GeneralMethods.removeBlock(oldLegsBlock);
			}

			playEarthbendingSound(this.headBlock.getLocation());
			this.start();
		}
	}

	private void formArmor() {
		if (TempBlock.isTempBlock(this.headBlock)) {
			TempBlock.revertBlock(this.headBlock, Material.AIR);
		}
		if (TempBlock.isTempBlock(this.legsBlock)) {
			TempBlock.revertBlock(this.legsBlock, Material.AIR);
		}

		final ItemStack head = new ItemStack(Material.LEATHER_HELMET, 1);
		final ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE, 1);
		final ItemStack legs = new ItemStack(Material.LEATHER_LEGGINGS, 1);
		final ItemStack feet = new ItemStack(Material.LEATHER_BOOTS, 1);

		final LeatherArmorMeta imHead = (LeatherArmorMeta) head.getItemMeta();
		final LeatherArmorMeta imChest = (LeatherArmorMeta) chest.getItemMeta();
		final LeatherArmorMeta imLegs = (LeatherArmorMeta) legs.getItemMeta();
		final LeatherArmorMeta imFeet = (LeatherArmorMeta) feet.getItemMeta();

		final int colorint = COLORS.get(this.player.getName());
		imHead.setColor(Color.fromRGB(colorint));
		imChest.setColor(Color.fromRGB(colorint));
		imLegs.setColor(Color.fromRGB(colorint));
		imFeet.setColor(Color.fromRGB(colorint));

		head.setItemMeta(imHead);
		chest.setItemMeta(imChest);
		legs.setItemMeta(imLegs);
		feet.setItemMeta(imFeet);

		this.armor = new TempArmor(this.player, new ItemStack[] { head, chest, legs, feet });
	}

	@Override
	public void progress() {
		if (!this.active || this.bPlayer.isOnCooldown(this) || System.currentTimeMillis() > this.getStartTime() + this.maxDuration) {
			this.remove();
			return;
		} else if (!this.formed && System.currentTimeMillis() > this.getStartTime() + this.interval) {
			if (this.bPlayer.getBoundAbilityName().equalsIgnoreCase(this.getName())) {
				this.bPlayer.addCooldown(this);
				return;
			}
			this.formArmor();
			this.formed = true;
			this.startTime = System.currentTimeMillis();
		}
	}

	@Override
	public void remove() {
		super.remove();
		if (this.armor != null) {
			this.armor.revert();
		}
	}

	private void defineColors() {
		if (ProjectKorra.plugin.getConfig().getStringList("Properties.Chat.Colors.Available").contains(this.player.getName())) {
			COLORS.put(this.player.getName(), GeneralMethods.parseColor(ProjectKorra.plugin.getConfig().getString("Properties.Chat.Colors." + this.player.getName())));
		} else {
			final int colorint = GeneralMethods.getRandom().nextInt(0xffffff + 1);
			COLORS.put(this.player.getName(), colorint);
			ProjectKorra.plugin.getConfig().set("Properties.Chat.Colors." + this.player.getName(), String.format(Locale.ENGLISH, "#%06X", colorint));
			ProjectKorra.plugin.saveConfig();
		}
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent event) {
		Player player = event.getPlayer();
		EarthArmor earthArmor = EarthArmor.getAbility(player);
		if (earthArmor != null) {
			earthArmor.formArmor();
		}
	}

	public static void progressAll() {
		for (final EarthArmor earthArmor : getAbilities(EarthArmor.class)) {
			earthArmor.progress();
		}
	}

	public static boolean hasColor(String player) {
		return COLORS.containsKey(player);
	}

	public static Map<String, Integer> getColors() {
		return COLORS;
	}

	public static void removeAll() {
		for (final EarthArmor earthArmor : getAbilities(EarthArmor.class)) {
			earthArmor.remove();
		}
	}

	@Override
	public String getName() {
		return "EarthArmor";
	}

	@Override
	public Location getLocation() {
		if (this.headBlockLocation != null) {
			return this.headBlockLocation;
		} else if (this.legsBlockLocation != null) {
			return this.legsBlockLocation;
		}
		return null;
	}

	@Override
	public long getCooldown() {
		return this.cooldown;
	}

	@Override
	public String getDescription() {
		return "This ability allows an earthbender to create armor from earth and minerals. To use, simply sneak. "
				+ "This ability is bound to the armor slot. Upon sneaking, a player's armor will be replaced by earth armor. "
				+ "This will lessen all damage, except for fire damage. Additionally, when the user's health goes below a "
				+ "certain point, the earth armor will shatter, knocking back all nearby entities. Finally, if the user "
				+ "shifts again, the armor will be removed.";
	}

	@Override
	public String getInstructions() {
		return "Sneak to activate.";
	}

	public Material getHeadMaterial() {
		return this.headMaterial;
	}

	public Material getLegsMaterial() {
		return this.legsMaterial;
	}

	public double getGoldHearts() {
		return this.goldHearts;
	}

	public void setGoldHearts(final double goldHearts) {
		this.goldHearts = goldHearts;
	}
}
