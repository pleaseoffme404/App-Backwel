package com.backwell.api_service.modules.products.jooq.repo;


import com.backwell.api_service.common.exception.SystemException;

import java.util.Set;
import java.util.UUID;

public interface ItemProductCategoryPathCustomRepository {
    /**
     * Saves category path registrations for multiple items under a specific product.
     *
     * <p>This method creates new ItemCategoryPathRegistry entries for each provided item ID,
     * associating them with the given product ID and category path. The operation is atomic
     * and will fail if any of the items already has an existing category path registration.
     *
     * @param itemIds the set of UUID identifiers for the items to be registered; must not be null
     * @param productId the UUID identifier of the product to which the items belong
     * @param categoryPath an array of UUIDs representing the hierarchical category path
     *
     * @throws SystemException if any of the specified items already has an existing
     *         ItemCategoryPathRegistry entry (checked by {@link #checkAllNotExists(Set)})
     *
     * @implNote This method uses batch insertion via jOOQ's insertStep pattern. All values
     *           are inserted in a single database operation for performance.
     *
     * @see #checkAllNotExists(Set)
     */
    void saveForProduct(Set<UUID> itemIds, UUID productId, UUID[] categoryPath);

    void saveNewItemForProduct(UUID itemId, UUID productId, UUID[] categoryPath);

    void updateProductPath(UUID productId, UUID[] newCategoryPath);

    /**
     * Checks whether none of the specified items have existing category path registrations.
     *
     * <p>This method queries the ITEM_PRODUCT_CATEGORY_PATH table to verify that no registry
     * entries exist for any of the provided item IDs. It is typically used as a validation
     * step before inserting new registrations to prevent duplicate entries.
     *
     * @param itemIds a set of UUID identifiers for the items to check; must not be null or empty
     * @return {@code true} if none of the items have any existing category path registrations;
     *         {@code false} if at least one item already has a registration
     *
     * @throws IllegalArgumentException if the count query returns null (should never happen
     *         as the database count aggregation always returns a value, even if zero)
     *
     * @implNote The method uses a database count query with an IN clause to efficiently
     *           check all provided items in a single database round trip. The assertion
     *           ensures defensive programming against unexpected null returns from
     *           {@code fetchOne(0, int.class)}.
     *
     * @see #saveForProduct(Set, UUID, UUID[])
     */
    boolean checkAllNotExists(Set<UUID> itemIds);
}
