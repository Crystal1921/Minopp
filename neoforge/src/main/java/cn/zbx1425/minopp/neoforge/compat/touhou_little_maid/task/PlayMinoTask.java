package cn.zbx1425.minopp.neoforge.compat.touhou_little_maid.task;

import cn.zbx1425.minopp.Mino;
import cn.zbx1425.minopp.block.BlockEntityMinoTable;
import cn.zbx1425.minopp.block.BlockMinoTable;
import cn.zbx1425.minopp.game.ActionReport;
import cn.zbx1425.minopp.game.Card;
import cn.zbx1425.minopp.game.CardGame;
import cn.zbx1425.minopp.game.CardPlayer;
import cn.zbx1425.minopp.item.ItemHandCards;
import cn.zbx1425.minopp.neoforge.compat.touhou_little_maid.MemoryTypeRegister;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidCheckRateTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Random;

public class PlayMinoTask extends MaidCheckRateTask {
    CardPlayer cardPlayer;
    public PlayMinoTask() {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
                InitEntities.TARGET_POS.get(), MemoryStatus.VALUE_ABSENT));
    }

    @Override
    protected void start(@NotNull ServerLevel level, EntityMaid maid, long gameTimeIn) {
        maid.getBrain().getMemory(MemoryTypeRegister.TARGET_POS.get()).ifPresent(targetPos -> {
            BlockPos tablePos = targetPos.currentBlockPosition();
            BlockState tableState = level.getBlockState(tablePos);
            if (tableState.is(Mino.BLOCK_MINO_TABLE.get())) {
                BlockPos corePos = BlockMinoTable.getCore(tableState, tablePos);
                BlockEntity blockEntity = level.getBlockEntity(corePos);
                if (blockEntity instanceof BlockEntityMinoTable tableEntity) {
                    if (tableEntity.game != null) {
                        inGameLogic(tableEntity, maid);
                        return;
                    }
                    String model = maid.getModelId().split(":")[1];
                    String playerName = maid.hasCustomName() ? maid.getCustomName().getString() : model;
                    CardPlayer cardPlayer = new CardPlayer(maid.getUUID(), playerName);
                    this.cardPlayer = cardPlayer;
                    ItemStack handStack = new ItemStack(Mino.ITEM_HAND_CARDS.get());
                    handStack.set(Mino.DATA_COMPONENT_TYPE_CARD_GAME_BINDING.get(),
                            new ItemHandCards.CardGameBindingComponent(maid.getUUID(), Optional.of(corePos)));
                    maid.setItemInHand(InteractionHand.MAIN_HAND, handStack);
                    tableEntity.joinPlayerToTable(cardPlayer, maid.position());
                }
            } else {
                maid.getBrain().eraseMemory(InitEntities.TARGET_POS.get());
                maid.stopRiding();
                this.cardPlayer = null;
            }
        });
    }
    private void inGameLogic(BlockEntityMinoTable tableEntity, EntityMaid maid) {
        if (tableEntity.game != null) {
            if (tableEntity.game.players.get(tableEntity.game.currentPlayerIndex).equals(cardPlayer)) {
                CardPlayer realPlayer = tableEntity.game.deAmputate(cardPlayer);
                ActionReport result = performAI(tableEntity.game, realPlayer);
                tableEntity.handleActionResult(result, realPlayer, null);
            }
        }
    }

    public ActionReport performAI(CardGame game, CardPlayer realPlayer) {
        Card topCard = game.topCard;
        boolean forgetsMino = false;
        boolean shoutsMino = !forgetsMino && realPlayer.hand.size() <= 2;

            if (realPlayer.hand.size() <= 1) {
                return game.playNoCard(realPlayer);
            }

        // If we have a card of same number but different suit
        for (Card card : realPlayer.hand) {
            if (card.number == topCard.number && card.suit != topCard.getEquivSuit() && card.suit != Card.Suit.WILD) {
                ActionReport result = game.playCard(realPlayer, card, null, shoutsMino);
                if (!result.isFail) return result;
            }
        }
        // If we have a card of same suit
        for (Card card : realPlayer.hand) {
            if (card.suit == topCard.getEquivSuit() && card.suit != Card.Suit.WILD) {
                ActionReport result = game.playCard(realPlayer, card, null, shoutsMino);
                if (!result.isFail) return result;
            }
        }
        // If we have any other card
        for (Card card : realPlayer.hand) {
            if (card.canPlayOn(topCard)) {
                if (card.suit == Card.Suit.WILD) {
                    // Check which suit is most common in hand
                    int[] suitCount = new int[4];
                    for (Card handCard : realPlayer.hand) {
                        if (handCard.suit != Card.Suit.WILD) {
                            suitCount[handCard.suit.ordinal()]++;
                        }
                    }
                    Card.Suit mostCommonSuit = Card.Suit.values()[new Random().nextInt(0, 4)];
                    for (int i = 1; i < 4; i++) {
                        if (suitCount[i] > suitCount[mostCommonSuit.ordinal()]) {
                            mostCommonSuit = Card.Suit.values()[i];
                        }
                    }
                    ActionReport result = game.playCard(realPlayer, card, mostCommonSuit, shoutsMino);
                    if (!result.isFail) return result;
                } else {
                    ActionReport result = game.playCard(realPlayer, card, null, shoutsMino);
                    if (!result.isFail) return result;
                }
            }
        }
        // We're out of option
        return game.playNoCard(realPlayer);
    }
}