/*
 * BedWars2023 - A bed wars mini-game.
 * Copyright (C) 2024 Tomas Keuper
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact e-mail: contact@fyreblox.com
 */

package com.tomkeuper.bedwars.support.version.v26_3;

import com.mojang.datafixers.util.Pair;
import com.saicone.rtag.RtagItem;
import com.saicone.rtag.util.OptionalType;
import com.saicone.rtag.util.SkullTexture;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.arena.generator.IGeneratorAnimation;
import com.tomkeuper.bedwars.api.arena.shop.ShopHolo;
import com.tomkeuper.bedwars.api.arena.team.ITeam;
import com.tomkeuper.bedwars.api.arena.team.TeamColor;
import com.tomkeuper.bedwars.api.entity.Despawnable;
import com.tomkeuper.bedwars.api.entity.GeneratorHolder;
import com.tomkeuper.bedwars.api.events.player.PlayerKillEvent;
import com.tomkeuper.bedwars.api.hologram.containers.IHoloLine;
import com.tomkeuper.bedwars.api.hologram.containers.IHologram;
import com.tomkeuper.bedwars.api.language.Language;
import com.tomkeuper.bedwars.api.language.Messages;
import com.tomkeuper.bedwars.api.server.VersionSupport;
import com.tomkeuper.bedwars.support.version.common.VersionCommon;
import com.tomkeuper.bedwars.support.version.v26_3.despawnable.DespawnableAttributes;
import com.tomkeuper.bedwars.support.version.v26_3.despawnable.DespawnableFactory;
import com.tomkeuper.bedwars.support.version.v26_3.despawnable.DespawnableType;
import com.tomkeuper.bedwars.support.version.v26_3.hologram.HoloLine;
import com.tomkeuper.bedwars.support.version.v26_3.hologram.Hologram;
import com.tomkeuper.bedwars.utils.ReflectionUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Ladder;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.command.Command;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftArmorStand;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;

public final class v26_3 extends VersionSupport {

    private final DespawnableFactory despawnableFactory;

    public v26_3(Plugin plugin, String name) {
        super(plugin, name);
        loadDefaultEffects();
        this.despawnableFactory = new DespawnableFactory(this);
    }

    @Override
    public void registerCommand(String name, Command cmd) {
        ((CraftServer) getPlugin().getServer()).getCommandMap().register(name, cmd);
    }

    @Override
    public void sendTitle(Player p, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        p.sendTitle(title == null ? " " : title, subtitle == null ? " " : subtitle, fadeIn, stay, fadeOut);
    }

    @Override
    public void playAction(Player p, String text) {
        p.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                new TextComponent(ChatColor.translateAlternateColorCodes('&', text)
                )
        );
    }

    @Override
    public boolean isBukkitCommandRegistered(String name) {
        return ((CraftServer) getPlugin().getServer()).getCommandMap().getCommand(name) != null;
    }

    @Override
    public org.bukkit.inventory.ItemStack getItemInHand(@NotNull Player p) {
        return p.getInventory().getItemInMainHand();
    }

    @Override
    public void hideEntity(Entity e, Player p) {
        sendPacket(p, new ClientboundRemoveEntitiesPacket(e.getEntityId()));
    }

    @Override
    public void fakeDamagePlayer(Player e) {
        Location loc = e.getLocation();
        World world = e.getWorld();
        world.playSound(loc, Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
        e.playHurtAnimation(loc.getYaw());
    }

    @Override
    public boolean isArmor(org.bukkit.inventory.ItemStack itemStack) {
        if(itemStack == null)
            return false;

        return Tag.ITEMS_HEAD_ARMOR.isTagged(itemStack.getType()) || Tag.ITEMS_CHEST_ARMOR.isTagged(itemStack.getType())
                || Tag.ITEMS_LEG_ARMOR.isTagged(itemStack.getType()) || Tag.ITEMS_FOOT_ARMOR.isTagged(itemStack.getType()) || itemStack.getType() == materialElytra();
    }

    @Override
    public boolean isTool(org.bukkit.inventory.ItemStack itemStack) {
        if(itemStack == null)
            return false;

        return itemStack.hasItemMeta();
    }

    @Override
    public boolean isSword(org.bukkit.inventory.ItemStack itemStack) {
        if(itemStack == null)
            return false;

        return Tag.ITEMS_SWORDS.isTagged(itemStack.getType());
    }

    @Override
    public boolean isAxe(org.bukkit.inventory.ItemStack itemStack) {
        return itemStack != null && Tag.ITEMS_AXES.isTagged(itemStack.getType());
    }

    @Override
    public boolean isBow(org.bukkit.inventory.ItemStack itemStack) {
        return itemStack != null && itemStack.getType() == Material.BOW;
    }


    @Override
    public boolean isProjectile(org.bukkit.inventory.ItemStack itemStack) {
        // The item stack carries no entity on modern versions, as on 1.21.11
        return false;
    }

    @Override
    public boolean isInvisibilityPotion(org.bukkit.inventory.@NotNull ItemStack itemStack) {
        if (!itemStack.getType().equals(org.bukkit.Material.POTION)) return false;

        org.bukkit.inventory.meta.PotionMeta pm = (org.bukkit.inventory.meta.PotionMeta) itemStack.getItemMeta();

        return pm != null && pm.hasCustomEffects() && pm.hasCustomEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
    }

    @Override
    public void registerEntities() {

    }

    @Override
    public void spawnShop(Location loc, String name1, Iterable<Player> players, IArena arena) {
        Location l = loc.clone();

        if (l.getWorld() == null) return;
        Villager vlg = (Villager) l.getWorld().spawnEntity(loc, EntityType.VILLAGER);
        vlg.setAI(false);
        vlg.setRemoveWhenFarAway(false);
        vlg.setCollidable(false);
        vlg.setInvulnerable(true);
        vlg.setSilent(true);
    }

    @Override
    public void spawnShopHologram(Location loc, String name1, Iterable<Player> players, ITeam team) {
        HashMap<String, List<Player>> languagePlayers = new HashMap<>();

        for (Player p : players) {
            String iso = Language.getPlayerLanguage(p).getIso();
            if (!languagePlayers.containsKey(iso)) languagePlayers.put(iso, new ArrayList<>());
            languagePlayers.get(iso).add(p);
        }

        for (String iso : languagePlayers.keySet()) {
            Language lang = Language.getLang(iso);
            String[] text = (lang.l(name1) == null || lang.l(name1).isEmpty() ? lang.l(name1.replace(name1.split("\\.")[2], "default")) : lang.l(name1)).toArray(new String[0]);
            IHologram h = createHologram(languagePlayers.get(iso), loc, text);
            new ShopHolo(h, team, iso);
        }
    }

    @Override
    public double getDamage(org.bukkit.inventory.ItemStack i) {
        return new RtagItem(i).getOptional("generic.attackDamage").asDouble(0d);
    }

    @Override
    public void spawnSilverfish(Location loc, ITeam bedWarsTeam, double speed, double health, int despawn, double damage, int pathFindingTicks) {
        var attr = new DespawnableAttributes(DespawnableType.SILVERFISH, speed, health, damage, despawn);
        var entity = despawnableFactory.spawn(attr, loc, bedWarsTeam, pathFindingTicks);

        new Despawnable(
                entity,
                bedWarsTeam, despawn,
                Messages.SHOP_UTILITY_NPC_SILVERFISH_NAME,
                PlayerKillEvent.PlayerKillCause.SILVERFISH_FINAL_KILL,
                PlayerKillEvent.PlayerKillCause.SILVERFISH
        );
    }

    @Override
    public void spawnIronGolem(Location loc, ITeam bedWarsTeam, double speed, double health, int despawn, int pathFindingTicks) {
        var attr = new DespawnableAttributes(DespawnableType.IRON_GOLEM, speed, health, 4, despawn);
        var entity = despawnableFactory.spawn(attr, loc, bedWarsTeam, pathFindingTicks);
        new Despawnable(
                entity,
                bedWarsTeam, despawn,
                Messages.SHOP_UTILITY_NPC_IRON_GOLEM_NAME,
                PlayerKillEvent.PlayerKillCause.IRON_GOLEM_FINAL_KILL,
                PlayerKillEvent.PlayerKillCause.IRON_GOLEM
        );
    }

    @Override
    public void minusAmount(Player p, org.bukkit.inventory.@NotNull ItemStack i, int amount) {
        if (i.getAmount() - amount <= 0) {
            if (p.getInventory().getItemInOffHand().equals(i)) {
                p.getInventory().setItemInOffHand(null);
            } else {
                p.getInventory().removeItem(i);
            }
            return;
        }
        i.setAmount(i.getAmount() - amount);
        //noinspection UnstableApiUsage
        p.updateInventory();
    }

    @Override
    public void setSource(TNTPrimed tnt, Player owner) {
        tnt.setSource(owner);
    }

    @Override
    public void voidKill(Player p) {
        p.damage(1000.0);
    }

    @Override
    public void hideArmor(@NotNull Player victim, Player receiver) {
        var air = new org.bukkit.inventory.ItemStack(Material.AIR);
        receiver.sendEquipmentChange(victim, Map.of(
                org.bukkit.inventory.EquipmentSlot.HEAD, air,
                org.bukkit.inventory.EquipmentSlot.CHEST, air,
                org.bukkit.inventory.EquipmentSlot.LEGS, air,
                org.bukkit.inventory.EquipmentSlot.FEET, air));
    }

    @Override
    public void showArmor(Player victim, Player receiver) {
        var inventory = victim.getInventory();
        var items = new HashMap<org.bukkit.inventory.EquipmentSlot, org.bukkit.inventory.ItemStack>();
        items.put(org.bukkit.inventory.EquipmentSlot.HEAD, orAir(inventory.getHelmet()));
        items.put(org.bukkit.inventory.EquipmentSlot.CHEST, orAir(inventory.getChestplate()));
        items.put(org.bukkit.inventory.EquipmentSlot.LEGS, orAir(inventory.getLeggings()));
        items.put(org.bukkit.inventory.EquipmentSlot.FEET, orAir(inventory.getBoots()));
        receiver.sendEquipmentChange(victim, items);
    }

    private static org.bukkit.inventory.ItemStack orAir(org.bukkit.inventory.ItemStack item) {
        return item == null ? new org.bukkit.inventory.ItemStack(Material.AIR) : item;
    }

    @Override
    public EnderDragon spawnDragon(Location l, ITeam team) {
        if (l == null || l.getWorld() == null) {
            getPlugin().getLogger().log(Level.WARNING, "Could not spawn Dragon. Location is null");
            return null;
        }
        EnderDragon ed = (EnderDragon) l.getWorld().spawnEntity(l, EntityType.ENDER_DRAGON);
        ed.setPhase(EnderDragon.Phase.CIRCLING);
        return ed;
    }

    @Override
    public void colorBed(ITeam team) {
        Location bedLoc = team.getBed();
        if (bedLoc == null) return;
        Material bedMaterial = team.getColor().bedMaterial();
        if (bedMaterial == null) return;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block bedBlock = bedLoc.clone().add(x, 0, z).getBlock();
                BlockData blockData = bedBlock.getBlockData();
                if (!(blockData instanceof org.bukkit.block.data.type.Bed)) continue;
                BlockFace facing = ((org.bukkit.block.data.type.Bed) blockData).getFacing();
                org.bukkit.block.data.type.Bed.Part part = ((org.bukkit.block.data.type.Bed) blockData).getPart();
                bedBlock.setType(bedMaterial, false);
                blockData = bedBlock.getBlockData();
                if (blockData instanceof org.bukkit.block.data.type.Bed bed) {
                    bed.setFacing(facing);
                    bed.setPart(part);
                    bedBlock.setBlockData(bed, false);
                }
            }
        }
    }

    @Override
    public void registerTntWhitelist(float endStoneBlast, float glassBlast) {
        try {
            Field field = ReflectionUtils.findField(BlockBehaviour.class, "explosionResistance");
            field.set(Blocks.END_STONE, endStoneBlast);
            field.set(Blocks.OBSIDIAN, glassBlast);
            var glass = new ArrayList<>(Blocks.STAINED_GLASS.asList());
            glass.add(Blocks.GLASS);
            for (var block : glass) field.set(block, glassBlast);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }

    @Override
    public float getBlastResistance(org.bukkit.block.Block bukkitBlock) {
        net.minecraft.world.level.block.Block nmsBlock = CraftMagicNumbers.getBlock(bukkitBlock.getType());
        return nmsBlock == null ? 0 : nmsBlock.getExplosionResistance();
    }

    @Override
    public void setBlockTeamColor(Block block, TeamColor teamColor) {
        if (block.getType().toString().contains("STAINED_GLASS") || block.getType().toString().equals("GLASS")) {
            block.setType(teamColor.glassMaterial());
        } else if (block.getType().toString().contains("_TERRACOTTA")) {
            block.setType(teamColor.glazedTerracottaMaterial());
        } else if (block.getType().toString().contains("_WOOL")) {
            block.setType(teamColor.woolMaterial());
        }
    }

    @Override
    public void setCollide(Player p, IArena a, boolean value) {
        p.setCollidable(value);
    }

    @Override
    public org.bukkit.inventory.ItemStack addCustomData(org.bukkit.inventory.ItemStack i, String data) {
        return RtagItem.edit(i, tag -> {
            tag.set(data, VersionSupport.PLUGIN_TAG_GENERIC_KEY);
        });
    }

    @Override
    public org.bukkit.inventory.ItemStack setTag(org.bukkit.inventory.ItemStack itemStack, String key, String value) {
        return RtagItem.edit(itemStack, tag -> {
            tag.set(value, key);
        });
    }

    @Override
    public String getTag(org.bukkit.inventory.ItemStack itemStack, String key) {
        var tag = new RtagItem(itemStack).getOptional(key);
        return tag.isEmpty() ? null : tag.asString(null);
    }

    @Override
    public boolean isCustomBedWarsItem(org.bukkit.inventory.ItemStack i) {
        if (i == null) return false;
        if (i.getType() == org.bukkit.Material.AIR) return false;
        RtagItem rtagItem = new RtagItem(i);
        OptionalType tag = rtagItem.getOptional(VersionSupport.PLUGIN_TAG_GENERIC_KEY);
        return tag.isNotEmpty();
    }

    @Override
    public String getCustomData(org.bukkit.inventory.ItemStack i) {
        RtagItem rtagItem = new RtagItem(i);
        OptionalType tag = rtagItem.getOptional(VersionSupport.PLUGIN_TAG_GENERIC_KEY);
        return (tag.isEmpty() || tag.isNotInstance(String.class)) ? null : tag.asString(null);
    }

    @Override
    public org.bukkit.inventory.ItemStack colourItem(org.bukkit.inventory.ItemStack itemStack, ITeam bedWarsTeam) {
        if (itemStack == null) return null;
        String type = itemStack.getType().toString();
        if (isBed(itemStack.getType())) {
            return new org.bukkit.inventory.ItemStack(bedWarsTeam.getColor().bedMaterial(), itemStack.getAmount());
        } else if (type.contains("_STAINED_GLASS_PANE")) {
            return new org.bukkit.inventory.ItemStack(bedWarsTeam.getColor().glassPaneMaterial(), itemStack.getAmount());
        } else if (type.contains("STAINED_GLASS") || type.equals("GLASS")) {
            return new org.bukkit.inventory.ItemStack(bedWarsTeam.getColor().glassMaterial(), itemStack.getAmount());
        } else if (type.contains("_TERRACOTTA")) {
            return new org.bukkit.inventory.ItemStack(bedWarsTeam.getColor().glazedTerracottaMaterial(), itemStack.getAmount());
        } else if (type.contains("_WOOL")) {
            return new org.bukkit.inventory.ItemStack(bedWarsTeam.getColor().woolMaterial(), itemStack.getAmount());
        }
        return itemStack;
    }

    @Override
    public org.bukkit.inventory.ItemStack createItemStack(String material, int amount, short data) {
        org.bukkit.inventory.ItemStack i;
        try {
            i = new org.bukkit.inventory.ItemStack(org.bukkit.Material.valueOf(material), amount);
        } catch (Exception ex) {
            getPlugin().getLogger().log(Level.WARNING, material + " is not a valid " + getName() + " material!");
            i = new org.bukkit.inventory.ItemStack(org.bukkit.Material.BEDROCK);
        }
        return i;
    }

    @Override
    public Material materialFireball() {
        return org.bukkit.Material.FIRE_CHARGE;
    }

    @Override
    public org.bukkit.Material materialPlayerHead() {
        return org.bukkit.Material.PLAYER_HEAD;
    }

    @Override
    public org.bukkit.Material materialSnowball() {
        return org.bukkit.Material.SNOWBALL;
    }

    @Override
    public org.bukkit.Material materialGoldenHelmet() {
        return org.bukkit.Material.GOLDEN_HELMET;
    }

    @Override
    public org.bukkit.Material materialGoldenChestPlate() {
        return org.bukkit.Material.GOLDEN_CHESTPLATE;
    }

    @Override
    public org.bukkit.Material materialGoldenLeggings() {
        return org.bukkit.Material.GOLDEN_LEGGINGS;
    }

    @Override
    public org.bukkit.Material materialNetheriteHelmet() {
        return Material.NETHERITE_HELMET;
    }

    @Override
    public org.bukkit.Material materialNetheriteChestPlate() {
        return Material.NETHERITE_CHESTPLATE;
    }

    @Override
    public org.bukkit.Material materialNetheriteLeggings() {
        return Material.NETHERITE_LEGGINGS;
    }

    @Override
    public org.bukkit.Material materialElytra() {
        return Material.ELYTRA;
    }

    @Override
    public org.bukkit.Material materialCake() {
        return org.bukkit.Material.CAKE;
    }

    @Override
    public org.bukkit.Material materialCraftingTable() {
        return org.bukkit.Material.CRAFTING_TABLE;
    }

    @Override
    public org.bukkit.Material materialEnchantingTable() {
        return org.bukkit.Material.ENCHANTING_TABLE;
    }

    @Override
    public org.bukkit.Material materialEndStone() {
        return Material.END_STONE;
    }

    @Override
    public org.bukkit.Material woolMaterial() {
        return org.bukkit.Material.WHITE_WOOL;
    }

    @Override
    public void setJoinSignBackground(BlockState b, Material material) {
        if (b.getBlockData() instanceof WallSign) {
            b.getBlock().getRelative(((WallSign) b.getBlockData()).getFacing().getOppositeFace()).setType(material);
        }
    }

    @Override
    public org.bukkit.inventory.ItemStack redGlassPane(int amount) {
        return new org.bukkit.inventory.ItemStack(Material.RED_STAINED_GLASS_PANE, amount);
    }

    @Override
    public org.bukkit.inventory.ItemStack greenGlassPane(int amount) {
        return new org.bukkit.inventory.ItemStack(Material.LIME_STAINED_GLASS_PANE, amount);
    }

    @Override
    public String getShopUpgradeIdentifier(org.bukkit.inventory.ItemStack itemStack) {
        RtagItem item = new RtagItem(itemStack);
        OptionalType tag = item.getOptional(VersionSupport.PLUGIN_TAG_TIER_KEY);
        return tag.isEmpty() ? "null" : tag.asString("null");
    }

    @Override
    public org.bukkit.inventory.ItemStack setShopUpgradeIdentifier(org.bukkit.inventory.ItemStack itemStack, String identifier) {
        RtagItem item = new RtagItem(itemStack);
        item.set(identifier, VersionSupport.PLUGIN_TAG_TIER_KEY);
        item.load();
        return item.getItem();
    }

    @Override
    public org.bukkit.inventory.ItemStack getPlayerHead(Player player, org.bukkit.inventory.ItemStack copyTagFrom) {
        org.bukkit.inventory.ItemStack head = SkullTexture.getTexturedHead(player.getUniqueId().toString());

        if (copyTagFrom != null) {
            Object tag = new RtagItem(copyTagFrom).getTag();
            RtagItem rtagItem = new RtagItem(head);
            rtagItem.set(tag, VersionSupport.PLUGIN_TAG_GENERIC_KEY);
            rtagItem.load();
            head = rtagItem.getItem();
        }

        return head;
    }

    @Override
    public void sendPlayerSpawnPackets(Player respawned, IArena arena) {
        if (respawned == null) return;
        if (arena == null) return;
        if (!arena.isPlayer(respawned)) return;

        // if method was used when the player was still in re-spawning screen
        if (arena.getRespawnSessions().containsKey(respawned)) return;

        ServerPlayer entityPlayer = getPlayer(respawned);
        ClientboundAddEntityPacket show = newPacketPlayOutSpawnEntity(entityPlayer);
        ClientboundSetEntityMotionPacket playerVelocity = new ClientboundSetEntityMotionPacket(entityPlayer);
        // we send head rotation packet because sometimes on respawn others see him with bad rotation
        ClientboundRotateHeadPacket head = new ClientboundRotateHeadPacket(entityPlayer, getCompressedAngle(entityPlayer.getBukkitYaw()));

        // retrieve current armor and in-hand items
        // we send a packet later for timing issues where other players do not see them
        List<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> list = getPlayerEquipment(entityPlayer);


        for (Player p : arena.getPlayers()) {
            if (p == null) continue;
            if (p.equals(respawned)) continue;
            // if p is in re-spawning screen continue
            if (arena.getRespawnSessions().containsKey(p)) continue;

            ServerPlayer boundTo = getPlayer(p);
            if (p.getWorld().equals(respawned.getWorld())) {
                if (respawned.getLocation().distance(p.getLocation()) <= arena.getRenderDistance()) {

                    // send respawned player to regular players
                    sendPackets(
                            p, show, head, playerVelocity,
                            new ClientboundSetEquipmentPacket(respawned.getEntityId(), list)
                    );

                    // send nearby players to respawned player
                    // if the player has invisibility hide armor
                    if (p.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                        hideArmor(p, respawned);
                    } else {
                        ClientboundAddEntityPacket show2 = newPacketPlayOutSpawnEntity(boundTo);

                        ClientboundSetEntityMotionPacket playerVelocity2 = new ClientboundSetEntityMotionPacket(boundTo);
                        ClientboundRotateHeadPacket head2 = new ClientboundRotateHeadPacket(boundTo, getCompressedAngle(boundTo.getBukkitYaw()));
                        sendPackets(respawned, show2, playerVelocity2, head2);
                        showArmor(p, respawned);
                    }
                }
            }
        }

        for (Player spectator : arena.getSpectators()) {
            if (spectator == null) continue;
            if (spectator.equals(respawned)) continue;
            respawned.hidePlayer(getPlugin(), spectator);
            if (spectator.getWorld().equals(respawned.getWorld())) {
                if (respawned.getLocation().distance(spectator.getLocation()) <= arena.getRenderDistance()) {

                    // send respawned player to spectator
                    sendPackets(
                            spectator, show, playerVelocity,
                            new ClientboundSetEquipmentPacket(respawned.getEntityId(), list),
                            new ClientboundRotateHeadPacket(entityPlayer, getCompressedAngle(entityPlayer.getBukkitYaw()))
                    );
                }
            }
        }
    }

    @Override
    public String getInventoryName(@NotNull InventoryEvent e) {
        return e.getView().getTitle();
    }

    @Override
    public void setUnbreakable(ItemMeta itemMeta) {
        itemMeta.setUnbreakable(true);
    }

    @Override
    public int getVersion() {
        return 14;
    }

    @Override
    public void registerVersionListeners() {
        new VersionCommon(this);
    }

    @Override
    public String getMainLevel() {
        //noinspection deprecation
        return ((DedicatedServer) MinecraftServer.getServer()).getProperties().levelName;
    }

    @Override
    public Fireball setFireballDirection(Fireball fireball, Vector vector) {
        fireball.setDirection(vector);
        return fireball;
    }

    @Override
    public void playRedStoneDot(Player player) {
        var at = player.getLocation().add(0, 2.6, 0);
        var dust = new Particle.DustOptions(Color.RED, 1);
        for (Player p : player.getWorld().getPlayers()) {
            if (p.equals(player)) continue;
            p.spawnParticle(Particle.DUST, at, 1, 0, 0, 0, 0, dust, true);
        }
    }

    @Override
    public void clearArrowsFromPlayerBody(Player player) {
        // minecraft clears them on death on newer version
    }

    @Override
    public Block placeTowerBlocks(@NotNull Block b, @NotNull IArena a, @NotNull TeamColor color, int x, int y, int z) {
        b.getRelative(x, y, z).setType(color.woolMaterial());
        a.addPlacedBlock(b.getRelative(x, y, z));
        return b.getRelative(x, y, z);
    }

    @Override
    public Block placeLadder(@NotNull Block b, int x, int y, int z, @NotNull IArena a, int ladderData) {
        Block block = b.getRelative(x, y, z);  //ladder block
        block.setType(Material.LADDER);
        if(block.getBlockData() instanceof Ladder ladder) {
            a.addPlacedBlock(block);
            switch (ladderData) {
                case 2 -> {
                    ladder.setFacing(BlockFace.NORTH);
                    block.setBlockData(ladder);
                }
                case 3 -> {
                    ladder.setFacing(BlockFace.SOUTH);
                    block.setBlockData(ladder);
                }
                case 4 -> {
                    ladder.setFacing(BlockFace.WEST);
                    block.setBlockData(ladder);
                }
                case 5 -> {
                    ladder.setFacing(BlockFace.EAST);
                    block.setBlockData(ladder);
                }
            }
        }
        return b;
    }

    @Override
    public void playVillagerEffect(Player player, Location location) {
        player.spawnParticle(Particle.HAPPY_VILLAGER, location, 1);
    }

    @Override
    public IHologram createHologram(Player p, Location location, String... lines) {
        List<String> linesList = new ArrayList<>(Arrays.asList(lines));
        // holograms are reversed, correcting that here
        Collections.reverse(linesList);
        return new Hologram(p, location, linesList);
    }

    @Override
    public IHologram createHologram(Player p, Location location, IHoloLine... lines) {
        List<IHoloLine> linesList = new ArrayList<>(Arrays.asList(lines));
        // holograms are reversed, correcting that here
        Collections.reverse(linesList);
        return new Hologram(p, linesList, location);
    }

    @Override
    public IHologram createHologram(Iterable<Player> players, Location location, String... lines) {
        List<String> linesList = new ArrayList<>(Arrays.asList(lines));
        // holograms are reversed, correcting that here
        Collections.reverse(linesList);
        return new Hologram(players, linesList, location);
    }

    @Override
    public IHologram createHologram(Iterable<Player> players, Location location, IHoloLine... lines) {
        List<IHoloLine> linesList = new ArrayList<>(Arrays.asList(lines));
        // holograms are reversed, correcting that here
        Collections.reverse(linesList);
        return new Hologram(players, location, linesList);
    }

    @Override
    public IHoloLine lineFromText(String text, @NotNull IHologram hologram) {
        return new HoloLine(text, hologram);
    }

    @Override
    public IGeneratorAnimation createDefaultGeneratorAnimation(ArmorStand armorStand) {
        return new DefaultGenAnimation(armorStand);
    }

    @Override
    public void updatePacketArmorStand(GeneratorHolder gh, Iterable<Player> players) {
        ArmorStand armorStand = gh.getArmorStand();
        var nmsEntity = ((CraftArmorStand) armorStand).getHandle();
        var spawn = newPacketPlayOutSpawnEntity(nmsEntity);
        var metadata = metadata(nmsEntity);
        var equipment = new ClientboundSetEquipmentPacket(armorStand.getEntityId(),
                Collections.singletonList(new Pair<>(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(gh.getHelmet()))));
        for (Player p : players) {
            sendPackets(p, spawn, metadata, equipment);
        }
    }

    @Override
    public void updatePacketArmorStandEquipment(GeneratorHolder generatorHolder) {
        ArmorStand armorStand = generatorHolder.getArmorStand();
        var equipment = new ClientboundSetEquipmentPacket(armorStand.getEntityId(),
                Collections.singletonList(new Pair<>(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(generatorHolder.getHelmet()))));
        var metadata = metadata(((CraftArmorStand) armorStand).getHandle());
        for (Player p : armorStand.getWorld().getPlayers()) {
            sendPackets(p, equipment, metadata);
        }
    }

    public static ClientboundSetEntityDataPacket metadata(net.minecraft.world.entity.Entity entity) {
        var values = entity.getEntityData().getNonDefaultValues();
        return new ClientboundSetEntityDataPacket(entity.getId(), values == null ? new ArrayList<>() : values);
    }

    @Override
    public void callPlayerDeathEvent(Player player, List<org.bukkit.inventory.ItemStack> drops, int droppedExp, int newLevel, String deathMessage) {
        DamageSource ds = DamageSource.builder(DamageType.GENERIC).build();
        PlayerDeathEvent deathEvent = new PlayerDeathEvent(player, ds, drops, droppedExp, newLevel, deathMessage);
        Bukkit.getPluginManager().callEvent(deathEvent);
    }

    @Override
    public float getAbsorption(Player player) {
        return (float) player.getAbsorptionAmount();
    }

    @Override
    public void destroyPacketArmorStand(GeneratorHolder generatorHolder, Iterable<Player> players) {
        var destroy = new ClientboundRemoveEntitiesPacket(generatorHolder.getArmorStand().getEntityId());
        for (Player p : players) {
            sendPacket(p, destroy);
        }
    }

    @Override
    public ArmorStand createPacketArmorStand(Location loc, Iterable<Player> players) {
        if (loc.getWorld() == null) throw new RuntimeException("World of a location should not be null.");
        var nmsEntity = new net.minecraft.world.entity.decoration.ArmorStand(((CraftWorld) loc.getWorld()).getHandle(), loc.getX(), loc.getY(), loc.getZ());
        nmsEntity.setPos(loc.getX(), loc.getY(), loc.getZ());
        var spawn = newPacketPlayOutSpawnEntity(nmsEntity);
        for (Player p : players) {
            sendPacket(p, spawn);
        }
        return new CraftArmorStand((CraftServer) getPlugin().getServer(), nmsEntity);
    }

    public static void sendPacket(Player player, Packet<?> packet) {
        ((CraftPlayer) player).getHandle().connection.send(packet);
    }

    public static void sendPackets(Player player, Packet<?> @NotNull ... packets) {
        ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;
        for (Packet<?> p : packets) {
            connection.send(p);
        }
    }

    public ServerPlayer getPlayer(Player player) {
        return ((CraftPlayer) player).getHandle();
    }

    public List<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> getPlayerEquipment(@NotNull ServerPlayer entityPlayer) {
        List<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> list = new ArrayList<>();
        for (var slot : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            list.add(new Pair<>(slot, entityPlayer.getItemBySlot(slot)));
        }
        return list;
    }

    public static ClientboundAddEntityPacket newPacketPlayOutSpawnEntity(net.minecraft.world.entity.Entity nmsEntity) {
        return new ClientboundAddEntityPacket(
                nmsEntity.getId(),
                nmsEntity.getUUID(),
                nmsEntity.getX(),
                nmsEntity.getY(),
                nmsEntity.getZ(),
                nmsEntity.getXRot(),
                nmsEntity.getYRot(),
                nmsEntity.getType(),
                0,
                nmsEntity.getDeltaMovement(),
                nmsEntity.getYHeadRot()
        );
    }
}
