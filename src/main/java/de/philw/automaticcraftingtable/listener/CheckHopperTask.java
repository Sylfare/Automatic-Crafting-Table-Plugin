package de.philw.automaticcraftingtable.listener;

import de.philw.automaticcraftingtable.AutomaticCraftingTable;
import de.philw.automaticcraftingtable.manager.CraftingTableManager;
import de.philw.automaticcraftingtable.util.Direction;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CheckHopperTask implements Runnable {

    private final AutomaticCraftingTable automaticCraftingTable;
    private final CraftingTableManager craftingTableManager;

    public CheckHopperTask (AutomaticCraftingTable automaticCraftingTable) {
        this.automaticCraftingTable = automaticCraftingTable;
        this.craftingTableManager = automaticCraftingTable.getCraftingTableManager();
    }

    /**
     * The run method is the "main" method of the plugin. It checks every registered crafting table and when
     * there is a hopper connected, who has enough items for the wanted recipe in it the crafting table gives the
     * wanted item to the next connected hopper.
     */

    @Override
    public void run() {
        for (String string: automaticCraftingTable.getCraftingTableManager().getLocations()) {
            Location location = craftingTableManager.getLocationFromSavedString(string);
            if (location == null) {
                continue;
            }
            Block craftingTable = Objects.requireNonNull(location.getWorld()).getBlockAt(location);
            if (craftingTable.getType() != Material.CRAFTING_TABLE) {
                continue;
            }
            for (Hopper hopper: getHoppersWhereItemComesFrom(craftingTable)) {
                if (hopper.getInventory().isEmpty()) {
                    continue;
                }

                CraftingTableManager craftingTableManager = automaticCraftingTable.getCraftingTableManager();
                if (craftingTableManager.isCraftingTableNotRegistered(craftingTable.getLocation())) {
                    craftingTableManager.addEmptyCraftingTable(craftingTable.getLocation());
                    craftingTableManager.saveCraftingTables();
                }

                List<ItemStack> contents = new ArrayList<>();

                for (int i = 0; i < 9; i++) {
                    contents.add(i, craftingTableManager.getItemFromIndex(craftingTable.getLocation(), i) == null ? null :
                            craftingTableManager.getItemFromIndex(craftingTable.getLocation(), i));
                }

                ItemStack wantItemStack = automaticCraftingTable.getRecipeUtil().getCraftResult(contents);

                if (wantItemStack == null) {
                    continue;
                }

                Hopper target = getNextTarget(craftingTable, hopper, wantItemStack);

                if (target == null) {
                    continue;
                }

                boolean accepted = true;

                List<ItemStack> ingredientList =
                        automaticCraftingTable.getRecipeUtil().getIngredientList(craftingTable.getLocation());

                for (ItemStack itemStack : ingredientList) {
                    if (!hopper.getInventory().containsAtLeast(itemStack, itemStack.getAmount())) {
                        accepted = false;
                    }
                }
                if (accepted) {
                    wantItemStack.setAmount(1);
                    target.getInventory().addItem(wantItemStack);
                    for (ItemStack itemStack : ingredientList) {
                        hopper.getInventory().removeItem(itemStack);
                    }
                }
            }
        }
    }

    /**
     * Checks if a hopper is Full
     *
     * @param hopper        The hopper you want to check
     * @param wantItemStack The item you want to transport (it is needed because it can be stacked)
     * @return If the hopper is full
     */

    private boolean hopperIsNotFull(Hopper hopper, ItemStack wantItemStack) {
        List<ItemStack> storageContents = new ArrayList<>();
        for (ItemStack isItemStack : hopper.getInventory().getStorageContents()) {
            if (isItemStack != null) {
                storageContents.add(isItemStack);
            }
        }
        if (storageContents.size() != 5) {
            return true;
        }
        for (ItemStack isItemStack : storageContents) {
            if (isItemStack.getMaxStackSize() == isItemStack.getAmount()) continue;
            wantItemStack.setAmount(isItemStack.getAmount());
            if (wantItemStack.isSimilar(isItemStack)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Checks if another hopper is connected and if so returns it
     *
     * @param craftingTable The Crafting Table
     * @param wantItemStack The item what should be transported
     * @param hopper The hopper where the item is coming from
     * @return The hopper down (first choice) or to the hopper in the opposite direction (second choice) or to the
     * next connected hopper (third choice) or null
     */

    private Hopper getNextTarget(Block craftingTable, Hopper hopper, ItemStack wantItemStack) {
        Hopper target;

        if (craftingTable.getWorld().getBlockAt(craftingTable.getLocation().subtract(0, 1, 0)).getType() == Material.HOPPER) {
            target =
                    (Hopper) craftingTable.getWorld().getBlockAt(craftingTable.getLocation().subtract(0, 1, 0)).getState();
            if (hopperIsNotFull(target, wantItemStack)) {
                return target;
            }
        }

        if (!hopperIsFacing(hopper, Direction.DOWN)) {
            if (hopperIsFacing(hopper, Direction.NORTH)
                    && craftingTable.getWorld().getBlockAt(craftingTable.getLocation().subtract(0, 0, 1)).getType() == Material.HOPPER) {
                target =
                        (Hopper) craftingTable.getWorld().getBlockAt(craftingTable.getLocation().subtract(0, 0, 1)).getState();
                if (hopperIsFacing(target, Direction.NORTH) && hopperIsNotFull(target, wantItemStack)) return target;
            }
            if (hopperIsFacing(hopper, Direction.SOUTH)
                    && craftingTable.getWorld().getBlockAt(craftingTable.getLocation().add(0
                    , 0, 1)).getType() == Material.HOPPER) {
                target =
                        (Hopper) craftingTable.getWorld().getBlockAt(craftingTable.getLocation().add(0, 0, 1)).getState();
                if (hopperIsFacing(target, Direction.SOUTH) && hopperIsNotFull(target, wantItemStack)) return target;
            }
            if (hopperIsFacing(hopper, Direction.WEST) // Hopper is facing west
                    && craftingTable.getWorld().getBlockAt(craftingTable.getLocation().subtract(1, 0, 0)).getType() == Material.HOPPER) {
                target =
                        (Hopper) craftingTable.getWorld().getBlockAt(craftingTable.getLocation().subtract(1, 0, 0)).getState();
                if (hopperIsFacing(target, Direction.WEST) && hopperIsNotFull(target, wantItemStack)) return target;
            }
            if (hopperIsFacing(hopper, Direction.EAST)
                    && craftingTable.getWorld().getBlockAt(craftingTable.getLocation().add(1,
                    0, 0)).getType() == Material.HOPPER) {
                target =
                        (Hopper) craftingTable.getWorld().getBlockAt(craftingTable.getLocation().add(1, 0, 0)).getState();
                if (hopperIsFacing(target, Direction.EAST) && hopperIsNotFull(target, wantItemStack)) return target;
            }
        }

        if (craftingTable.getWorld().getBlockAt(craftingTable.getLocation().subtract(0, 0, 1)).getType() == Material.HOPPER) {
            target =
                    (Hopper) craftingTable.getWorld().getBlockAt(craftingTable.getLocation().subtract(0, 0, 1)).getState();
            if (target.getLocation() != hopper.getLocation() && hopperIsNotFull(target, wantItemStack) && hopperIsFacing(target, Direction.NORTH)) return target;
        }
        if (craftingTable.getWorld().getBlockAt(craftingTable.getLocation().add(0, 0, 1)).getType() == Material.HOPPER) {
            target = (Hopper) craftingTable.getWorld().getBlockAt(craftingTable.getLocation().add(0, 0, 1)).getState();
            if (target.getLocation() != hopper.getLocation() && hopperIsNotFull(target, wantItemStack) && hopperIsFacing(target, Direction.SOUTH)) return target;
        }
        if (craftingTable.getWorld().getBlockAt(craftingTable.getLocation().subtract(1, 0, 0)).getType() == Material.HOPPER) {
            target =
                    (Hopper) craftingTable.getWorld().getBlockAt(craftingTable.getLocation().subtract(1, 0, 0)).getState();
            if (target.getLocation() != hopper.getLocation() && hopperIsNotFull(target, wantItemStack) && hopperIsFacing(target, Direction.WEST)) return target;
        }
        if (craftingTable.getWorld().getBlockAt(craftingTable.getLocation().add(1, 0, 0)).getType() == Material.HOPPER) {
            target = (Hopper) craftingTable.getWorld().getBlockAt(craftingTable.getLocation().add(1, 0, 0)).getState();
            if (target.getLocation() != hopper.getLocation() && hopperIsNotFull(target, wantItemStack) && hopperIsFacing(target, Direction.EAST)) return target;
        }

        return null;
    }

    private ArrayList<Hopper> getHoppersWhereItemComesFrom(Block craftingTable) {
        ArrayList<Hopper> hoppersWhereItemsComesFrom = new ArrayList<>();
        if (craftingTable.getWorld().getBlockAt(craftingTable.getLocation().add(0, 1, 0)).getType() == Material.HOPPER) {
            Hopper hopper = (Hopper) craftingTable.getWorld().getBlockAt(craftingTable.getLocation().add(0, 1, 0)).getState();
            if (hopperIsFacing(hopper, Direction.DOWN)) {
                hoppersWhereItemsComesFrom.add(hopper);
            }
        }
        if (craftingTable.getWorld().getBlockAt(craftingTable.getLocation().add(1, 0, 0)).getType() == Material.HOPPER) {
            Hopper hopper = (Hopper) craftingTable.getWorld().getBlockAt(craftingTable.getLocation().add(1, 0, 0)).getState();
            if (hopperIsFacing(hopper, Direction.WEST)) {
                hoppersWhereItemsComesFrom.add(hopper);
            }
        }
        if (craftingTable.getWorld().getBlockAt(craftingTable.getLocation().subtract(1, 0, 0)).getType() == Material.HOPPER) {
            Hopper hopper = (Hopper) craftingTable.getWorld().getBlockAt(craftingTable.getLocation().subtract(1, 0, 0)).getState();
            if (hopperIsFacing(hopper, Direction.EAST)) {
                hoppersWhereItemsComesFrom.add(hopper);
            }
        }
        if (craftingTable.getWorld().getBlockAt(craftingTable.getLocation().add(0, 0, 1)).getType() == Material.HOPPER) {
            Hopper hopper = (Hopper) craftingTable.getWorld().getBlockAt(craftingTable.getLocation().add(0, 0, 1)).getState();
            if (hopperIsFacing(hopper, Direction.NORTH)) {
                hoppersWhereItemsComesFrom.add(hopper);
            }
        }
        if (craftingTable.getWorld().getBlockAt(craftingTable.getLocation().subtract(0, 0, 1)).getType() == Material.HOPPER) {
            Hopper hopper = (Hopper) craftingTable.getWorld().getBlockAt(craftingTable.getLocation().subtract(0, 0, 1)).getState();
            if (hopperIsFacing(hopper, Direction.SOUTH)) {
                hoppersWhereItemsComesFrom.add(hopper);
            }
        }
        return hoppersWhereItemsComesFrom;
    }

    private boolean hopperIsFacing(Hopper hopper, Direction direction) {
        if (direction == Direction.DOWN) {
            return hopper.getRawData() == 0 || hopper.getRawData() == 8;
        }
        if (direction == Direction.NORTH) {
            return hopper.getRawData() == 2 || hopper.getRawData() == 10;
        }
        if (direction == Direction.SOUTH) {
            return hopper.getRawData() == 3 || hopper.getRawData() == 11;
        }
        if (direction == Direction.WEST) {
            return hopper.getRawData() == 4 || hopper.getRawData() == 12;
        }
        if (direction == Direction.EAST) {
            return hopper.getRawData() == 5 || hopper.getRawData() == 13;
        }
        return false;
    }

}
