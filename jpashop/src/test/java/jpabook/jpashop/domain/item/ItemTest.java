package jpabook.jpashop.domain.item;

import jpabook.jpashop.exception.NotEnoughStockException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemTest {

    @Test
    public void 재고를_감소시킨다() throws Exception {
        // given
        Item item = createBook();
        int quantity = 3;

        // when
        item.removeStock(quantity);
        
        // then
        Assertions.assertEquals(7, item.getStockQuantity());
    }

    @Test
    public void 재고보다_많은_수량감소() throws Exception {
        // given
        Item item = createBook();
        item.setStockQuantity(10);

        int quantity = 11;

        // when & then
        assertThrows(NotEnoughStockException.class, () -> {
            item.removeStock(quantity);
        });
    }

    private Item createBook() {
        Item item = new Book();
        item.setStockQuantity(10);
        return item;
    }
}
