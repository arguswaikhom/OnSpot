package com.crown.onspot.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.crown.library.onspotlibrary.model.ListItem
import com.crown.library.onspotlibrary.model.cart.OSCart
import com.crown.library.onspotlibrary.utils.emun.BusinessItemStatus
import com.crown.library.onspotlibrary.views.OSCartQuantityView

class SelectProductViewModel : ViewModel() {
    private val cart: MutableLiveData<List<ListItem>> = MutableLiveData()
    private val removedCart: MutableLiveData<HashSet<String>> = MutableLiveData()

    /**
     * This method should call only when the product data changed on the server database
     *
     * @param updatedCart: the updated product list
     */
    @Synchronized
    fun setCart(updatedCart: List<ListItem>) {
        val temp = cart.value
        if (temp == null) {
            cart.value = updatedCart
            return
        }

        /**
         * 1 -> Keep user selected product quantities while product data changed
         * 2 -> If any user selected product is unavailable, leave the quantity as 0 and add it to the removed set
         */
        var hasProductRemoved = false
        val removedSet = removedCart.value ?: HashSet()
        for (newItem: ListItem in updatedCart) {
            for (oldItem: ListItem in temp) {
                if ((newItem as OSCart).itemId == (oldItem as OSCart).itemId && oldItem.quantity > 0) {
                    // Check if one of the user selected product is unavailable
                    if (BusinessItemStatus.isAvailable(newItem.status)) {
                        // Add the selected quantity to the right product
                        newItem.quantity = oldItem.quantity
                    } else {
                        // User selected item is unavailable
                        hasProductRemoved = true
                        removedSet.add(oldItem.itemId)
                    }
                }
            }
        }
        if (hasProductRemoved) removedCart.value = removedSet
        cart.value = updatedCart
    }

    fun getCart(): MutableLiveData<List<ListItem>> {
        return this.cart
    }

    fun getRemovedCart(): MutableLiveData<HashSet<String>> {
        return this.removedCart
    }

    /**
     * Every time the user updates (add or subtract) product quantity, this method calls.
     *
     * Update the quantity of the product to display on the UI
     *
     * @param productId: ID of the product which the quantity is changing
     * @param mode: mode of the change [OSCartQuantityView.OnQuantityChangeListener.ADD] or [OSCartQuantityView.OnQuantityChangeListener.SUB]
     */
    @Synchronized
    fun updateQuantity(productId: String, mode: Int) {
        val temp = cart.value ?: return
        for (item: ListItem in temp) {
            if ((item as OSCart).itemId == productId) {
                if (mode == OSCartQuantityView.OnQuantityChangeListener.ADD) item.quantity += 1
                else if (mode == OSCartQuantityView.OnQuantityChangeListener.SUB) {
                    if (item.quantity <= 0) return
                    item.quantity -= 1
                }
                break
            }
        }
        cart.value = temp
    }
}