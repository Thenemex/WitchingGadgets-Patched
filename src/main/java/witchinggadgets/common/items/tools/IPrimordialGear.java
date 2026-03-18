package witchinggadgets.common.items.tools;

import net.minecraft.item.ItemStack;

public interface IPrimordialGear
{
	void cycleAbilities(ItemStack stack);
	int getAbility(ItemStack stack);
}