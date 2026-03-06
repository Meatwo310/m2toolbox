package net.meatwo310.m2toolbox.item;

import net.meatwo310.m2toolbox.M2ToolboxKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class AbstractContainerItem extends Item {
    public AbstractContainerItem(Item.Properties properties) {
        super(properties.stacksTo(1));
    }

    protected abstract AbstractContainerMenu createMenu(int id, Inventory inv, ItemStack stack);

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (stack.getCount() > 1) {
            if (!level.isClientSide) {
                player.sendSystemMessage(Component
                        .translatable(M2ToolboxKeys.ITEM_OVERSTACKED)
                        .withStyle(ChatFormatting.RED)
                );
            }
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            NetworkHooks.openScreen(
                    (ServerPlayer) player,
                    new SimpleMenuProvider(
                            (id, inv, p) -> createMenu(id, inv, stack),
                            stack.getHoverName()
                    ),
                    buf -> {
                        buf.writeItem(stack);
                        buf.writeBoolean(false); // fromToolbox (TrayMenu) / ignored by ToolboxMenu
                        buf.writeBoolean(false); // fromCurios
                    }
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static Component getCustomOrIndexedName(ItemStack stack, int index) {
        return getCustomOrIndexedName(stack, index, " %d");
    }

    public static Component getCustomOrIndexedName(ItemStack stack, int index, String format) {
        var name = stack.getHoverName();
        if (stack.hasCustomHoverName()) {
            return name;
        } else {
            return name.copy().append(format.formatted(index + 1));
        }
    }
}
