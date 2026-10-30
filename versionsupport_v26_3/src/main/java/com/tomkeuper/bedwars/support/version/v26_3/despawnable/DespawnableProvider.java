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

package com.tomkeuper.bedwars.support.version.v26_3.despawnable;

import com.tomkeuper.bedwars.api.arena.team.ITeam;
import com.tomkeuper.bedwars.api.server.VersionSupport;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class DespawnableProvider<T> {

    abstract DespawnableType getType();

    abstract String getDisplayName(DespawnableAttributes attr, ITeam team);

    abstract T spawn(@NotNull DespawnableAttributes attr, @NotNull Location location, @NotNull ITeam team, VersionSupport api, int pathFindingTicks);

    protected boolean notSameTeam(@NotNull Entity entity, ITeam team, @NotNull VersionSupport api) {
        var despawnable = api.getDespawnablesList().getOrDefault(entity.getBukkitEntity().getUniqueId(), null);
        return null == despawnable || despawnable.getTeam() != team;
    }

    protected GoalSelector getTargetSelector(@NotNull PathfinderMob mob) {
        return mob.targetSelector;
    }

    protected GoalSelector getGoalSelector(@NotNull PathfinderMob mob) {
        return mob.getGoalSelector();
    }

    protected void clearSelectors(@NotNull PathfinderMob mob) {
        mob.targetSelector.removeAllGoals(goal -> true);
        mob.getGoalSelector().removeAllGoals(goal -> true);
    }

    protected Goal getTargetGoal(Mob entity, ITeam team, VersionSupport api, int pathFindingTicks) {
        return new NearestAttackableTargetGoal<>(entity, LivingEntity.class, pathFindingTicks, true, false,
                (target, level) -> {
                    if (target instanceof net.minecraft.world.entity.player.Player player) {
                        var uuid = player.getBukkitEntity().getUniqueId();
                        return !player.getBukkitEntity().isDead() && !team.wasMember(uuid)
                                && !team.getArena().isReSpawning(uuid) && !team.getArena().isSpectator(uuid);
                    }
                    return notSameTeam(target, team, api);
                });
    }

    protected void applyDefaultSettings(org.bukkit.entity.@NotNull LivingEntity bukkitEntity, DespawnableAttributes attr,
                                        ITeam team) {
        bukkitEntity.setRemoveWhenFarAway(false);
        bukkitEntity.setPersistent(true);
        bukkitEntity.setCustomNameVisible(true);
        bukkitEntity.setCustomName(getDisplayName(attr, team));

        Objects.requireNonNull(bukkitEntity.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(attr.health());
        Objects.requireNonNull(bukkitEntity.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(attr.speed());
        Objects.requireNonNull(bukkitEntity.getAttribute(Attribute.ATTACK_DAMAGE)).setBaseValue(attr.damage());
    }
}
