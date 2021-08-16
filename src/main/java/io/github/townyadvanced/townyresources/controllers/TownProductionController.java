package io.github.townyadvanced.townyresources.controllers;

import com.gmail.goosius.siegewar.TownOccupationController;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import io.github.townyadvanced.townyresources.metadata.TownyResourcesGovernmentMetaDataController;
import io.github.townyadvanced.townyresources.objects.ResourceOffer;
import io.github.townyadvanced.townyresources.settings.TownyResourcesSettings;
import io.github.townyadvanced.townyresources.settings.TownyResourcesTranslation;
import io.github.townyadvanced.townyresources.util.TownyResourcesMessagingUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TownProductionController {

    private static List<ResourceOffer> allResourceOffers = new ArrayList<>();  //All resource offers
    private static int sumOfAllOfferDiscoveryProbabilityWeights = 0;  //Used during discovery
    
    private void load() {
        //Load all resource offers into memory
        sumOfAllOfferDiscoveryProbabilityWeights = 0;
        loadResourceOffers("ores", TownyResourcesSettings.getOffersOres());
        loadResourceOffers("trees", TownyResourcesSettings.getOffersTrees());
        loadResourceOffers("crops", TownyResourcesSettings.getOffersCrops());
        loadResourceOffers("animals", TownyResourcesSettings.getOffersAnimals());
        loadResourceOffers("monsters", TownyResourcesSettings.getOffersMonsters());   
    }
    
    public static List<String> getDiscoveredResources(Town town) {
        String resourcesString = TownyResourcesGovernmentMetaDataController.getDiscovered(town);
        if(resourcesString.isEmpty()) {
            return new ArrayList<>();
        } else {
            String[] resourcesArray = resourcesString.split(",");
            return Arrays.asList(resourcesArray);        
        }
    }

    public static void discoverNewResource(Resident resident, Town town, List<String> alreadyDiscoveredResources) throws TownyException {
 		//Ensure the offers list is not empty	
        if(allResourceOffers.isEmpty())
            throw new TownyException("msg_err_empty_offers_list");

        //Generate a random number to determine which offer will win
        int winningNumber = (int)((Math.random() * sumOfAllOfferDiscoveryProbabilityWeights) + 0.5);
        
        //Determine which  offer has won
        ResourceOffer winningCandidate = null;
        ResourceOffer candidate;
        ResourceOffer nextCandidate = null;
        for(int i = 0; i < allResourceOffers.size() - 1; i++) {      //Don't check the last entry 
            candidate = allResourceOffers.get(i);   
            nextCandidate = allResourceOffers.get(i+1);
            if(winningNumber >= candidate.getDiscoveryId() && winningNumber < nextCandidate.getDiscoveryId()) {
                winningCandidate = candidate;
                break;
            }
        }

        //If no winner was found yet, the only remaining candidate is the last entry, which must be the winner
        if(winningCandidate == null) {
            winningCandidate = nextCandidate;
        }

        //Discover the resource
        alreadyDiscoveredResources.add(winningCandidate.getMaterial());
        TownyResourcesGovernmentMetaDataController.setDiscovered(town, alreadyDiscoveredResources);
        town.save();
   
   		//Send global message
   		String translationkey = "discovery.message." + winningCandidate.getCategory();
		TownyResourcesMessagingUtil.sendGlobalMessage(TownyResourcesTranslation.of(translationkey, resident.getName(), town.getName()));
     
        recalculateTownProduction(town);
    }

    /**
     * Load all resource offers belonging to the a given category
     * 
     * @param offersCategory the given category
     * @param offersList the list of offers in the given category
     */
    private static void loadResourceOffers(String offersCategory, List<String> offersList) {
        String[] offerAsArray;
        String offerMaterial;
        int offerBaseAmount;
        int offerDiscoveryProbabilityWeight;
        int offerDiscoveryId;
        ResourceOffer newResourceOffer;
        
        for(String offer: offersList) {
            offerAsArray = offer.split("-");
            offerMaterial = offerAsArray[0];
            offerBaseAmount = Integer.parseInt(offerAsArray[1]);
            offerDiscoveryProbabilityWeight = Integer.parseInt(offerAsArray[2]);
            offerDiscoveryId = sumOfAllOfferDiscoveryProbabilityWeights;
            newResourceOffer = new ResourceOffer(offersCategory, offerMaterial, offerBaseAmount, offerDiscoveryProbabilityWeight, offerDiscoveryId);
            allResourceOffers.add(newResourceOffer);                
            sumOfAllOfferDiscoveryProbabilityWeights += offerDiscoveryProbabilityWeight; 
        }
    }

    /**
     * Recalculate production for a single town
     * Also do for its owner nation if it has one
     *         
     * @param town the town to recalculate production for
     */
    
    public static void recalculateTownProduction(Town town) {
        //Get production percentages
        List<Integer> productionBonusesPerLevel = TownyResourcesSettings.getProductionPercentagesPerResourceLevel();

        //Get discovered resources
        List<String> discoveredResources = getDiscoveredResources(town);

        //Determine owner nation
        Nation ownerNation = null;
        double townCutNormalized = 0;
        double nationCutNormalized = 0;
        if(TownOccupationController.isTownOccupied(town)) {
            ownerNation = TownOccupationController.getTownOccupier(town);
            nationCutNormalized = TownyResourcesSettings.getTownResourcesProductionNationTaxNormalized();
            townCutNormalized = 1 - nationCutNormalized;
        } else if (town.hasNation()) {
            ownerNation = TownyAPI.getInstance().getTownNationOrNull(town);
            nationCutNormalized = TownyResourcesSettings.getTownResourcesProductionNationTaxNormalized();
            townCutNormalized = 1 - nationCutNormalized;
        } 

        //Build the town production list
        List<String> townProduction = new ArrayList<>();
        String materialName;
        double baseProductionAmount;
        double finalProductionAmount;
        double resourceLevelProductionModifierNormalized;
   
        for(int i = 0; i < discoveredResources.size(); i++) {
            materialName = discoveredResources.get(i);
            baseProductionAmount = allResourceOffers.get(materialName).getBaseAmount();
            resourceLevelProductionModifierNormalized = (double)productionBonusesPerLevel.get(i) / 100;
            if(ownerNation == null) {
                finalProductionAmount = baseProductionAmount * resourceLevelProductionModifierNormalized;
            } else {
                finalProductionAmount = baseProductionAmount * resourceLevelProductionModifierNormalized * townCutNormalized;
            }            
            townProduction.add(materialName + "-" + finalProductionAmount);
        }
        
        //Save data
        TownyResourcesGovernmentMetaDataController.saveTownProduction(townProduction);
        
        //Recalculate for nation
        if(ownerNation != null) {
            //TODO
        }
      
    }
   
}
