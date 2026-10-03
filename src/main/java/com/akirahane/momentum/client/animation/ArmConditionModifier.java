package com.akirahane.momentum.client.animation;

import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractModifier;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

public class ArmConditionModifier extends AbstractModifier {
	private final Player player;
	private final Vector3f animationBodyRot = new Vector3f();

	public ArmConditionModifier(Player player) {
		this.player = player;
	}

	@Override
	public void get3DTransform(PlayerAnimBone bone) {
		String name = bone.getName();
		if ("body".equals(name)) {
			super.get3DTransform(bone);
			this.animationBodyRot.set(bone.rotation.x, bone.rotation.y, bone.rotation.z);
		} else if (this.shouldKeepVanillaArm(name)) {
			float vRotX = bone.rotation.x();
			float vRotY = bone.rotation.y();
			float vRotZ = bone.rotation.z();
			super.get3DTransform(bone);
			bone.rotation.x = vRotX - this.animationBodyRot.x();
			bone.rotation.y = vRotY - this.animationBodyRot.y();
			bone.rotation.z = vRotZ - this.animationBodyRot.z();
		} else {
			super.get3DTransform(bone);
		}
	}

	private boolean shouldKeepVanillaArm(String boneName) {
		boolean isUsing = this.player.isUsingItem();
		boolean isAttacking = this.player.swinging;
		if ("right_arm".equals(boneName)) {
			return isUsing && this.player.getUsedItemHand() == InteractionHand.MAIN_HAND || isAttacking || this.isRangedWeaponReady(this.player.getMainHandItem());
		} else {
			return !"left_arm".equals(boneName)
				? false
				: isUsing && this.player.getUsedItemHand() == InteractionHand.OFF_HAND || this.isRangedWeaponReady(this.player.getOffhandItem());
		}
	}

	private boolean isRangedWeaponReady(ItemStack stack) {
		return stack.isEmpty() ? false : stack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(stack);
	}
}
