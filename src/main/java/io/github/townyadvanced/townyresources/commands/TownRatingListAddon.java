package io.github.townyadvanced.townyresources.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.AddonCommand;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translator;
import com.palmergames.bukkit.towny.object.comparators.ComparatorCaches;
import com.palmergames.bukkit.towny.object.comparators.ComparatorType;
import com.palmergames.bukkit.util.ChatTools;
import io.github.townyadvanced.townyresources.metadata.TownyResourcesGovernmentMetaDataController;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;

public class TownRatingListAddon implements CommandExecutor {
	public TownRatingListAddon() {
		TownyCommandAddonAPI.addSubCommand(new AddonCommand(TownyCommandAddonAPI.CommandType.TOWN_LIST_BY, "rating", this));
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Translator translator = Translator.locale(sender);
		if (sender instanceof Player && !sender.hasPermission("towny.command.town.list.residents")) {
			TownyMessaging.sendErrorMsg(sender, translator.of("townyresources.build_rating.list_no_permission"));
			return true;
		}
		var lines = new ArrayList<>(ComparatorCaches.getTownListCache(ComparatorType.NAME));
		lines.sort(Comparator.comparingDouble((com.palmergames.util.Pair<java.util.UUID, Component> line) ->
				TownyResourcesGovernmentMetaDataController.getTownBuildRating(TownyAPI.getInstance().getTown(line.key()))).reversed());
		int pages = Math.max(1, (lines.size() + 9) / 10);
		int page = 1;
		try {
			boolean hasPage = false;
			for (String arg : args) {
				if (arg.equalsIgnoreCase("by") || arg.equalsIgnoreCase("rating"))
					continue;
				if (hasPage)
					throw new NumberFormatException();
				page = Integer.parseInt(arg);
				hasPage = true;
			}
			if (page < 1 || page > pages)
				throw new NumberFormatException();
		} catch (NumberFormatException e) {
			TownyMessaging.sendErrorMsg(sender, translator.of("townyresources.build_rating.list_usage", pages));
			return true;
		}
		TownyMessaging.sendMessage(sender, ChatTools.formatTitle(translator.of("town_plu")));
		sender.sendMessage(Component.text(translator.of("town_name"), NamedTextColor.DARK_AQUA)
				.append(Component.text(" - ", NamedTextColor.DARK_GRAY))
				.append(Component.text(translator.of("townyresources.build_rating.header"), NamedTextColor.DARK_AQUA)));
		for (int i = (page - 1) * 10; i < Math.min(page * 10, lines.size()); i++) {
			var line = lines.get(i);
			Town town = TownyAPI.getInstance().getTown(line.key());
			sender.sendMessage(line.value().replaceText(TextReplacementConfig.builder()
					.match("\\(\\d+\\)").once()
					.replacement("(" + TownyResourcesGovernmentMetaDataController.getTownBuildRating(town) + ")").build()));
		}
		sender.sendMessage(TownyMessaging.getPageNavigationFooter("towny:town list", page, "by rating", pages, translator));
		return true;
	}
}
