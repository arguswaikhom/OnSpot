package com.crown.onspot.view.viewholder

import android.content.Context
import android.content.Intent
import android.text.Html
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.crown.library.onspotlibrary.model.OSRating
import com.crown.library.onspotlibrary.model.cart.OSCart
import com.crown.library.onspotlibrary.page.ProductActivity
import com.crown.library.onspotlibrary.utils.BusinessItemUtils
import com.crown.library.onspotlibrary.utils.OSBusinessItemUnitUtils
import com.crown.library.onspotlibrary.utils.OSListUtils
import com.crown.library.onspotlibrary.utils.OSMessage
import com.crown.library.onspotlibrary.utils.emun.BusinessItemStatus
import com.crown.onspot.R
import com.crown.onspot.databinding.LiSelectProductBinding
import com.crown.onspot.model.SelectProductViewModel
import java.util.*

class SelectProductVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private var item: OSCart? = null
    private var productViewModel: SelectProductViewModel
    private var context: Context = itemView.context
    private var binding: LiSelectProductBinding = LiSelectProductBinding.bind(itemView)

    init {
        binding.descriptionDownIv.setOnClickListener(this::onCLickedDescriptionDown)
        binding.quantityQv.setOnQuantityChangeListener { view: View?, mode: Int -> onClickedQuantityChange(view, mode) }
        productViewModel = ViewModelProvider(context as ViewModelStoreOwner).get(SelectProductViewModel::class.java)
    }

    fun bind(cart: OSCart) {
        this.item = cart

        item?.let {
            binding.nameTv.text = it.itemName
            binding.nameTv.setOnClickListener(this::onCLickedItemName)
            if (TextUtils.isEmpty(it.category)) {
                binding.categoryTv.visibility = View.GONE
            } else {
                binding.categoryTv.visibility = View.VISIBLE
                binding.categoryTv.text = it.category
            }
            binding.quantityQv.setQuantity(it.quantity.toInt())
            Glide.with(context).load(if (OSListUtils.isEmpty(it.imageUrls)) null else it.imageUrls[0]).into(binding.imageContainerInclude.imageIv)

            val finalPrice = BusinessItemUtils.getFinalPrice(it.price).toInt()
            val priceWithTax = (it.price.price + BusinessItemUtils.getTaxAmount(it.price)).toInt()
            if (priceWithTax != finalPrice) {
                binding.actualPriceTv.isEnabled = false
                binding.finalPriceTv.visibility = View.VISIBLE
                binding.actualPriceTv.text = Html.fromHtml("<del>₹ $priceWithTax</del>")
                binding.finalPriceTv.text = String.format(Locale.ENGLISH, "₹ %s", finalPrice)
            } else {
                binding.actualPriceTv.isEnabled = true
                binding.finalPriceTv.visibility = View.GONE
                binding.actualPriceTv.text = String.format(Locale.ENGLISH, "₹ %s", priceWithTax)
            }

            val quantity = OSBusinessItemUnitUtils.getDisplayText((it.price.quantity as Long).toInt(), it.price.unit)
            val priceDisplay = "per <b>$quantity</b>"
            binding.unitTv.text = Html.fromHtml(priceDisplay)

            when (val status = if (it.status == null) BusinessItemStatus.AVAILABLE else it.status) {
                BusinessItemStatus.AVAILABLE -> {
                    binding.imageContainerInclude.availabilityTv.text = status.getName()
                    binding.imageContainerInclude.productStatusContent.setCardBackgroundColor(context.getColor(R.color.item_status_available))
                }
                BusinessItemStatus.NOT_AVAILABLE -> {
                    binding.imageContainerInclude.availabilityTv.text = status.getName()
                    binding.imageContainerInclude.productStatusContent.setCardBackgroundColor(context.getColor(R.color.item_status_not_available))
                }
                BusinessItemStatus.OUT_OF_STOCK -> {
                    binding.imageContainerInclude.availabilityTv.text = status.getName()
                    binding.imageContainerInclude.productStatusContent.setCardBackgroundColor(context.getColor(R.color.item_status_out_of_stock))
                }
            }

            val rating: OSRating? = it.productRating
            if (rating != null) {
                val avg: Double = if (rating.average != null) rating.average else 0.0
                binding.imageContainerInclude.productRating.rating = avg.toFloat()
            } else binding.imageContainerInclude.productRating.rating = 0F
        }
    }

    private fun onCLickedItemName(view: View) {
        item?.let {
            val intent = Intent(context, ProductActivity::class.java)
            intent.putExtra(ProductActivity.PRODUCT_ID, it.itemId)
            context.startActivity(intent)
        }
    }

    private fun onCLickedDescriptionDown(view: View) {
        item?.let {
            val message = if (TextUtils.isEmpty(it.description)) "No item description" else it.description
            AlertDialog.Builder(context).setTitle(it.itemName).setMessage(message).setPositiveButton("OK", null).create().show()
        }
    }

    private fun onClickedQuantityChange(view: View?, mode: Int) {
        item?.let {
            if (BusinessItemStatus.isAvailable((it.status))) {
                productViewModel.updateQuantity(it.itemId, mode)
            } else OSMessage.showSToast(context, "Product " + it.status.getName())
        }
    }
}