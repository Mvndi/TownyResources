package io.github.townyadvanced.townyresources.listeners;

import com.palmergames.bukkit.towny.event.NationRemoveTownEvent;
import com.palmergames.bukkit.towny.event.statusscreen.NationStatusScreenEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Translator;
import com.palmergames.bukkit.towny.TownyFormatter;
import io.github.townyadvanced.townyresources.controllers.TownResourceProductionController;
import io.github.townyadvanced.townyresources.metadata.TownyResourcesGovernmentMetaDataController;
import io.github.townyadvanced.townyresources.settings.TownyResourcesSettings;
import io.github.townyadvanced.townyresources.util.TownyResourcesMessagingUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * 
 * @author Goosius
 *
 */
public class TownyResourcesNationEventListener implements Listener {

    private static final String SCREEN_COMPONENT = "TownyResources";
    private static final String RESOURCES_LABEL = "townyresources.town.screen.resources";
    private static final String DAILY_PRODUCTION = "townyresources.nation.screen.daily.production";
    private static final String AVAILABLE_FOR_COLLECTION = "townyresources.nation.screen.available.for.collection";


	@EventHandler(priority = EventPriority.MONITOR)
	public void onNationRemoveTown(NationRemoveTownEvent event) {
		if (!TownyResourcesSettings.isEnabled())
			return;
		TownResourceProductionController.recalculateProductionForOneTown(event.getTown());
		if (event.getNation().exists())
			TownResourceProductionController.recalculateProductionForOneNation(event.getNation());
	}

    @EventHandler
    public void onNationStatusScreen(NationStatusScreenEvent event) {
        if (!TownyResourcesSettings.isEnabled())
            return;

        Translator translator = Translator.locale(event.getCommandSender());
        Nation nation = event.getNation();
        String productionAsString = TownyResourcesGovernmentMetaDataController.getDailyProduction(nation);
        String availableAsString = TownyResourcesGovernmentMetaDataController.getAvailableForCollection(nation);
        if (productionAsString.isEmpty() && availableAsString.isEmpty())
            return;

        Component details = TownyResourcesMessagingUtil.getResourceDetailsComponent(translator, productionAsString, DAILY_PRODUCTION)
            .appendNewline()
            .append(TownyResourcesMessagingUtil.getResourceDetailsComponent(translator, availableAsString, AVAILABLE_FOR_COLLECTION));
        event.getStatusScreen().addComponentOf(SCREEN_COMPONENT,
            TownyFormatter.colourHoverKey(translator.of(RESOURCES_LABEL)), HoverEvent.showText(details));
    }
}
