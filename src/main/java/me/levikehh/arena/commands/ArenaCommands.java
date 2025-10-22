package me.levikehh.arena.commands;

import me.levikehh.arena.models.Arena;
import me.levikehh.arena.models.PendingMatch;
import me.levikehh.arena.ArenaPlugin;
import me.levikehh.arena.database.MatchResultRepository;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import hu.nomindz.devkit.utils.*;
import net.md_5.bungee.api.chat.ClickEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import hu.nomindz.devkit.command.*;
import org.bukkit.plugin.java.JavaPlugin;

public final class ArenaCommands implements CommandProvider {
	private final Map<UUID, Location> pendingArenas = new HashMap<>();

	@Override
	public List<CommandBase> provide(JavaPlugin raw) {
		ArenaPlugin plugin = (ArenaPlugin) raw;

		CommandBase listArenas = CommandBase.of("list")
				.permission(ComplexPermission.any("arena.admin", "arena.use"))
				.executor((server, sender, args) -> {
					Player player = (Player) sender;
					Collection<Arena> arenas = plugin.getArenaManager().getAllArenas();

					if (arenas.isEmpty()) {
						player.sendMessage(MessageFormatter
								.error("No arenas have been created yet!"));
						return;
					}

					player.sendMessage(MessageFormatter.header("Arenas (" + arenas.size() + ")"));

					for (Arena arena : arenas) {
						String status = arena.isOccupied() ? "[OCCUPIED]" : "[AVAILABLE]";

						player.spigot().sendMessage(new MessageBuilder().addSuccess("• ")
								.addVariable(arena.getName())
								.addSuccess(" ").addVariable(status).build());
					}
				}).build();

		CommandBase createArena = CommandBase.of("create")
				.permission(ComplexPermission.all("arena.admin"))
				.param(Param.of("arenaName", ParamTypes.STRING).build())
				.executor((server, sender, args) -> {
					Player player = (Player) sender;
					String name = (String) args.get("arenaName");

					if (plugin.getArenaManager().arenaExists(name)) {
						player.spigot().sendMessage(
								new MessageBuilder().addError("Arena ").addVariable(name).addError(" already exists")
										.build());
						return;
					}

					UUID playerId = player.getUniqueId();

					if (!pendingArenas.containsKey(playerId)) {
						pendingArenas.put(playerId, player.getLocation());
						player.spigot().sendMessage(
								new MessageBuilder().addSuccess("Spawn point 1 set for arena ").addVariable(name)
										.build());
						player.spigot()
								.sendMessage(new MessageBuilder().addSuccess("Run this command again at spawn point 2")
										.build());
					} else {
						Location spawn1 = this.pendingArenas.remove(playerId);
						Location spawn2 = player.getLocation();

						plugin.getArenaManager().createArena(name, spawn1, spawn2).ifSuccess(v -> {
							player.spigot().sendMessage(
									new MessageBuilder().addSuccess("Arena ").addVariable(name).addSuccess(" created")
											.build());
						}).ifFailure(error -> {
							player.spigot().sendMessage(
									new MessageBuilder().addError("Failed to create arena: " + error).build());
						});
					}
				}).build();

		CommandBase removeArena = CommandBase.of("remove")
				.permission(ComplexPermission.all("arena.admin"))
				.param(Param.of("arenaName", ParamTypes.STRING)
						.suggestions((server, sender) -> plugin.getArenaManager().getAllArenas().stream()
								.map(arena -> arena.getName())
								.toList())
						.build())
				.executor((server, sender, args) -> {
					Player player = (Player) sender;
					String name = (String) args.get("arenaName");
					if (plugin.getArenaManager().arenaExists(name)) {
						player.spigot().sendMessage(
								new MessageBuilder().addError("Arena ")
										.addVariable(name)
										.addError(" doesn't exists").build());
						return;
					}

					plugin.getArenaManager().deleteArena(name).ifSuccess(v -> {
						player.spigot().sendMessage(
								new MessageBuilder().addSuccess("Arena ")
										.addVariable(name)
										.addSuccess(" deleted").build());
					}).ifFailure(error -> {
						player.spigot().sendMessage(new MessageBuilder()
								.addError("Failed to remove arena: " + error).build());
					});
				})
				.build();

		CommandBase initChallenge = CommandBase.of("challenge")
				.param(Param.of("target", ParamTypes.PLAYER)
						.suggestions((server, sender) -> server.getOnlinePlayers().stream()
								.filter(player -> !player.equals(sender))
								.map(player -> player.getDisplayName())
								.toList())
						.build())
				.executor((server, sender, args) -> {
					Player initiator = (Player) sender;
					// Check matches
					if (plugin.getMatchManager().isInMatch(initiator)) {
						initiator.spigot()
								.sendMessage(new MessageBuilder()
										.addError("You can't start a new fight while participating in one")
										.build());
						return;
					}

					Player target = (Player) args.get("target");
					if (target == null) {
						initiator.spigot()
								.sendMessage(new MessageBuilder().addError(
										"Player not found. Maybe they are offline?")
										.build());
						return;
					}

					if (initiator.equals(target)) {
						initiator.spigot()
								.sendMessage(new MessageBuilder().addError(
										"You can't challenge yourself")
										.build());
						return;
					}

					if (plugin.getMatchManager().isInMatch(target)) {
						initiator.spigot()
								.sendMessage(new MessageBuilder()
										.addError("You can't challenge a player whos already in a fight")
										.build());
						return;
					}

					// Check pending matches
					PendingMatch pendingMatch = plugin.getMatchManager().getPendingMatches()
							.stream()
							.filter(match -> match.getInitiator().equals(target)
									&& match.getTarget().equals(initiator))
							.findFirst()
							.orElse(null);

					if (pendingMatch != null) {
						this.acceptChallenge(plugin, initiator, target);
						return;
					}

					pendingMatch = new PendingMatch(initiator, target);
					if (!plugin.getMatchManager().setPendingMatch(pendingMatch)) {
						return;
					}

					MessageBuilder message = new MessageBuilder();

					message
							.addVariable(initiator.getDisplayName())
							.addSuccess(" challenged you for a PvP Arena match.\n")
							.addSuccess("Do you ")
							.addClickable("accept",
									new ClickEvent(ClickEvent.Action.RUN_COMMAND,
											"/arena accept " + initiator
													.getDisplayName()),
									null)
							.addSuccess(" or ")
							.addClickable("decline",
									new ClickEvent(ClickEvent.Action.RUN_COMMAND,
											"/arena decline " + initiator
													.getDisplayName()),
									null)
							.addSuccess(" it?");

					target.spigot().sendMessage(message.build());
					initiator.spigot().sendMessage(
							new MessageBuilder().addSuccess("You successfully challenged ")
									.addVariable(target.getDisplayName())
									.addSuccess("!").build());
				}).build();

		CommandBase acceptChallenge = CommandBase.of("accept")
				.param(Param.of("initiator", ParamTypes.PLAYER)
						.suggestions((server, sender) -> plugin.getMatchManager().getPendingMatches().stream()
								.filter(match -> match.getTarget().equals(sender))
								.map(match -> match.getInitiator().getDisplayName())
								.toList())
						.build())
				.executor((server, sender, args) -> {
					Player player = (Player) sender;
					Player initiator = (Player) args.get("initiator");
					if (initiator == null) {
						player.spigot()
								.sendMessage(new MessageBuilder().addError(
										"Player not found. Maybe they are offline?")
										.build());
						return;
					}

					this.acceptChallenge(plugin, player, initiator);
				}).build();

		CommandBase declineChallenge = CommandBase.of("decline")
				.param(Param.of("initiator", ParamTypes.PLAYER)
						.suggestions((server, sender) -> plugin.getMatchManager().getPendingMatches().stream()
								.filter(match -> match.getTarget().equals(sender))
								.map(match -> match.getInitiator().getDisplayName())
								.toList())
						.build())
				.executor((server, sender, args) -> {
					Player player = (Player) sender;
					Player initiator = (Player) args.get("initiator");
					if (initiator == null) {
						player.spigot()
								.sendMessage(
										new MessageBuilder().addError(
												"Player not found. Maybe they are offline?")
												.build());
						return;
					}

					// Check if there is a pending match
					PendingMatch pendingMatch = plugin.getMatchManager().getPendingMatches()
							.stream()
							.filter(match -> match.getInitiator().equals(initiator))
							.findFirst().orElse(null);

					if (pendingMatch == null) {
						player.spigot().sendMessage(
								new MessageBuilder().addError(
										"Could not find pending challenge with this player")
										.build());
						return;
					}

					if (plugin.getMatchManager().removePendingMatch(initiator, player)) {
						player.sendMessage(ChatColor.YELLOW + "Match against " + ChatColor.GRAY
								+
								initiator.getDisplayName() + " has been declined");
						initiator.sendMessage(ChatColor.GRAY + player.getDisplayName() +
								" declined the match");
					} else {
						player.spigot().sendMessage(MessageBuilder.internalError());
					}
				}).build();

		CommandBase playerStats = CommandBase.of("stats")
				.param(Param.of("player", ParamTypes.PLAYER)
						.suggestions((server, sender) -> server.getOnlinePlayers().stream()
								.filter(player -> !player.equals(sender))
								.map(player -> player.getDisplayName())
								.toList())
						.optional()
						.build())
				.executor((server, sender, args) -> {
					Player player = (Player) sender;
					Player target = (Player) args.get("player");

					Player statsFor = target == null ? player : target;

					MatchResultRepository.getInstance(plugin.getDatabaseManager())
							.getPlayerStats(statsFor.getUniqueId())
							.ifSuccess(stats -> {
								player.sendMessage(
										MessageFormatter.header(statsFor.getDisplayName() + "'s Statistics"));
								player.sendMessage(ChatColor.YELLOW + "Total Matches: " + ChatColor.WHITE +
										stats.totalMatches);
								player.sendMessage(ChatColor.GREEN + "Wins: " + ChatColor.WHITE + stats.wins
										+
										ChatColor.GRAY + " (" + String.format("%.1f%%", stats.getWinRate()) + ")");
								player.sendMessage(ChatColor.RED + "Losses: " + ChatColor.WHITE +
										stats.losses);
								player.sendMessage(ChatColor.YELLOW + "Draws: " + ChatColor.WHITE +
										stats.draws);
								player.sendMessage(ChatColor.AQUA + "K/D Ratio: " + ChatColor.WHITE +
										String.format("%.2f", stats.getKDRatio()));
								player.sendMessage(ChatColor.GRAY + "Total Playtime: " +
										TimeFormatter.formatTimeReadable(stats.totalPlaytimeSeconds));
							})
							.ifFailure(error -> {
								player.sendMessage(MessageFormatter.error("Failed to load stats: " + error));
							});
				})
				.build();

		CommandBase playerHistory = CommandBase.of("history")
				.param(Param.of("player", ParamTypes.PLAYER)
						.suggestions((server, sender) -> server.getOnlinePlayers().stream()
								.filter(player -> !player.equals(sender))
								.map(player -> player.getDisplayName())
								.toList())
						.optional()
						.build())
				.executor((server, sender, args) -> {
					Player player = (Player) sender;
					Player target = (Player) args.get("player");

					Player statsFor = target == null ? player : target;

					MatchResultRepository.getInstance(plugin.getDatabaseManager())
							.getPlayerMatchHistory(statsFor.getUniqueId(), 10)
							.ifSuccess(matches -> {
								if (matches.isEmpty()) {
									player.sendMessage(ChatColor.YELLOW + statsFor.getDisplayName() +
											" has no match history yet!");
									return;
								}

								player.sendMessage(MessageFormatter.header(statsFor.getDisplayName() +
										"'s Recent Matches"));

								for (MatchResultRepository.MatchResultData match : matches) {
									String opponent;
									boolean isPlayer1 = match.player1Uuid.equals(statsFor.getUniqueId().toString());

									if (isPlayer1) {
										opponent = match.player2Name;
									} else {
										opponent = match.player1Name;
									}

									// Determine result for this player
									String resultStr;
									if (match.winnerUuid == null) {
										resultStr = ChatColor.YELLOW + "DRAW";
									} else if (match.winnerUuid.equals(statsFor.getUniqueId().toString())) {
										resultStr = ChatColor.GREEN + "WIN";
									} else {
										resultStr = ChatColor.RED + "LOSS";
									}

									player.sendMessage(resultStr + ChatColor.GRAY + " vs " + ChatColor.WHITE +
											opponent +
											ChatColor.GRAY + " in " + match.arenaName +
											" (" + TimeFormatter.formatTime(match.durationSeconds) + ")");
								}
							})
							.ifFailure(error -> {
								player.sendMessage(MessageFormatter.error("Failed to load history: " +
										error));
							});
				})
				.build();

		CommandBase arenaLeaderboard = CommandBase.of("leaderboard")
				.executor((server, sender, args) -> {
					Player player = (Player) sender;
					MatchResultRepository.getInstance(plugin.getDatabaseManager()).getLeaderboard(10)
							.ifSuccess(leaderboard -> {
								if (leaderboard.isEmpty()) {
									player.sendMessage(ChatColor.YELLOW + "No players have competed yet!");
									return;
								}

								player.sendMessage(MessageFormatter.header("Top 10 Players"));

								int rank = 1;
								for (MatchResultRepository.PlayerStats stats : leaderboard) {
									String rankStr = ChatColor.GOLD + "#" + rank + ". ";
									String nameStr = ChatColor.WHITE + stats.playerName;
									String statsStr = ChatColor.GREEN + " " + stats.wins + "W " +
											ChatColor.RED + stats.losses + "L " +
											ChatColor.YELLOW + stats.draws + "D " +
											ChatColor.GRAY + "(" + String.format("%.1f%%", stats.getWinRate()) + ")";

									player.sendMessage(rankStr + nameStr + statsStr);
									rank++;
								}
							})
							.ifFailure(error -> {
								player.sendMessage(MessageFormatter.error("Failed to load leaderboard: " +
										error));
							});
				})
				.build();

		CommandBase arenaRoot = CommandBase.of("arena")
				.child(listArenas)
				.child(createArena)
				.child(removeArena)
				.child(initChallenge)
				.child(acceptChallenge)
				.child(declineChallenge)
				.child(playerStats)
				.child(playerHistory)
				.child(arenaLeaderboard)
				.build();

		return List.of(arenaRoot);
	}

	private void acceptChallenge(ArenaPlugin plugin, Player player, Player initiator) {
		PendingMatch pendingMatch = plugin.getMatchManager().getPendingMatches()
				.stream()
				.filter(match -> match.getInitiator().equals(initiator))
				.findFirst().orElse(null);

		if (pendingMatch == null) {
			player.spigot().sendMessage(
					new MessageBuilder().addError(
							"Could not find pending challenge with this player")
							.build());
			return;
		}

		// Start match
		List<Arena> availableArenas = plugin.getArenaManager().getAvailableArenas();
		if (availableArenas.isEmpty()) {
			player.spigot().sendMessage(new MessageBuilder()
					.addError("All the arenas are occupied").build());
			return;
		}

		Arena arena = availableArenas.get(0);
		plugin.getMatchManager().startMatch(arena, initiator, player);
	}
}