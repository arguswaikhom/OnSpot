package com.crown.onspot.page

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.crown.library.onspotlibrary.controller.OSPreferences
import com.crown.library.onspotlibrary.model.ListItem
import com.crown.library.onspotlibrary.model.cart.OSCart
import com.crown.library.onspotlibrary.model.user.UserOS
import com.crown.library.onspotlibrary.page.BusinessActivity
import com.crown.library.onspotlibrary.utils.*
import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey
import com.crown.onspot.R
import com.crown.onspot.controller.AppController
import com.crown.onspot.databinding.ActivitySelectProductBinding
import com.crown.onspot.model.SelectProductViewModel
import com.crown.onspot.view.ListItemAdapter
import com.google.firebase.firestore.*
import com.google.gson.Gson
import java.util.*
import kotlin.collections.ArrayList

class SelectProductActivity : AppCompatActivity() {
    companion object {
        const val BUSINESS_ID = "BUSINESS_ID"
    }

    private lateinit var user: UserOS
    private var bussId: String? = null
    private lateinit var productViewModel: SelectProductViewModel
    private lateinit var adapter: ListItemAdapter
    private val dataset = ArrayList<ListItem>()
    private lateinit var binding: ActivitySelectProductBinding
    private var productListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        /**
         * Product selection page required to authenticate user.
         * If the app is not authenticated; Guide the user to login.
         * Otherwise, continue.
         */
        if (!AppController.getInstance().isAuthenticated) {
            handleAuthReqd()
            return
        }
        user = OSPreferences.getInstance(applicationContext).getObject(OSPreferenceKey.USER, UserOS::class.java)

        handleIntent()
        if (TextUtils.isEmpty(bussId)) {
            handleFailedContent()
            return
        }

        adapter = ListItemAdapter(dataset)
        binding.productRv.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        binding.productRv.adapter = adapter

        prepareDataset()

        productViewModel = ViewModelProvider(this).get(SelectProductViewModel::class.java)
        productViewModel.getCart().observe(this, { handleProductChanges(this, it) })
        productViewModel.getRemovedCart().observe(this, { handleRemovedCart(this, it) })

        binding.selectionSummaryInclude.orderBtn.setOnClickListener(this::onClickedOrder)
    }

    /**
     * There are 2 ways to open this activity
     * 1 -> From the app itself to select product
     * 2 -> From the inApp link shared
     *
     * [bussId] should be filled by one of these activity call
     */
    private fun handleIntent() {
        // In-app activity call
        bussId = intent.getStringExtra(BUSINESS_ID)
        if (!TextUtils.isEmpty(bussId)) return

        // App link activity call
        val appLinkAction = intent.action
        val appLinkData = intent.data

        if (appLinkAction == Intent.ACTION_VIEW && appLinkData != null) {
            val paths = OSUrlUtils.getPaths(appLinkData)

            // * First path "order-online", second path "{business-ref-id}"
            // If the url doesn't have 2 paths, this activity won't handle those urls
            if (paths.size == 2) bussId = appLinkData.lastPathSegment
        }
    }

    /**
     * If the contents are not available, display the info view and hide the main content
     */
    private fun handleFailedContent() {
        binding.contentUnavailableIllv.showButton(false)
        binding.contentUnavailableIllv.setIllustration(R.drawable.ill_undraw_not_found_60pq)
        binding.contentUnavailableIllv.setText(getString(R.string.msg_info_content_not_found))
        showFailedContentView()
    }

    private fun handleAuthReqd() {
        binding.contentUnavailableIllv.showButton(true)
        binding.contentUnavailableIllv.setButtonText("Login")
        binding.contentUnavailableIllv.setIllustration(R.drawable.ill_undraw_access_account_re_8spm)
        binding.contentUnavailableIllv.setText(getString(R.string.msg_info_auth_reqd))
        binding.contentUnavailableIllv.setOnActionClickListener { navToLoginPage() }
        showFailedContentView()
    }

    private fun navToLoginPage() {
        startActivity(Intent(this, SignInActivity::class.java))
        finish()
    }

    private fun handleClosedBusiness(bussName: String?) {
        binding.contentUnavailableIllv.showButton(true)
        binding.contentUnavailableIllv.setButtonText("Business Profile")
        binding.contentUnavailableIllv.setIllustration(R.drawable.ill_undraw_shopping_app_flsj)
        val des = HtmlCompat.fromHtml(getString(R.string.msg_info_business_closed, bussName), HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
        binding.contentUnavailableIllv.setText(des)
        binding.contentUnavailableIllv.setOnActionClickListener {
            val intent = Intent(this, BusinessActivity::class.java)
            intent.putExtra(BusinessActivity.BUSINESS_ID, bussName)
            startActivity(intent)
        }
        showFailedContentView()
    }

    private fun showFailedContentView() {
        binding.contentUnavailableIllv.visibility = View.VISIBLE
        binding.mainContent.visibility = View.GONE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            if (!AppController.getInstance().isAuthenticated) navToLoginPage()
            else onBackPressed()
            return true
        } else if (item.itemId == R.id.nav_oooa_call_business) {
            if (AppController.getInstance().isAuthenticated) {
                OSContactReacher.getBusinessMobileNumber(this, bussId, { value: String? ->
                    if (!this.isFinishing) OSCommonIntents.onIntentCallRequest(this, value!!)
                }) { _: java.lang.Exception?, _: String? ->
                    OSMessage.showSBar(this, getString(R.string.msg_get_contact_failed))
                }
                return true
            } else navToLoginPage()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        productListener?.remove()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.option_order_online_activity, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Get business details from the server.
     */
    private fun prepareDataset() {
        // Get business details
        OSFirebaseDocUtils.getBusiness(bussId!!) { doc: DocumentSnapshot?, _: Exception? ->
            doc?.let {
                val hod: Boolean? = doc.get(OSString.fieldHodAvailable) as Boolean?
                val isOpen: Boolean? = doc.get(OSString.fieldIsOpen) as Boolean?
                val isActive: Boolean? = doc.get(OSString.fieldIsActive) as Boolean?
                val adminBlocked: Boolean? = doc.get(OSString.fieldAdminBlocked) as Boolean?
                val bussName: String? = doc.get(OSString.fieldDisplayName) as String?

                if (isActive == null || !isActive || (adminBlocked != null && adminBlocked)) {
                    handleFailedContent()
                    return@getBusiness
                }

                if (isOpen != null && !isOpen) {
                    handleClosedBusiness(bussName)
                    return@getBusiness
                }

                // Let the user know that home delivery is not available for this business
                if (hod == null || !hod) binding.hodNotAvailableTv.visibility = View.VISIBLE

                // Get products
                productListener = FirebaseFirestore.getInstance().collection(OSString.refItem)
                        .whereEqualTo(OSString.fieldBusinessRefId, bussId)
                        .whereEqualTo(OSString.fieldIsActive, true)
                        .whereEqualTo(OSString.fieldAdminBlocked, false)
                        .whereEqualTo(OSString.fieldArchived, false)
                        .addSnapshotListener(this::handleProductSnapshots)
            }
        }
    }

    private fun handleProductSnapshots(snapshots: QuerySnapshot?, e: FirebaseFirestoreException?) {
        if (snapshots == null || snapshots.isEmpty) handleFailedContent()
        else {
            val productList = ArrayList<ListItem>()
            for (doc: DocumentSnapshot in snapshots.documents) {
                val product = doc.toObject(OSCart::class.java) ?: continue
                productList.add(product)
            }
            productList.sortWith { o1: ListItem, o2: ListItem -> (o1 as OSCart).itemName.compareTo((o2 as OSCart).itemName, ignoreCase = true) }
            productViewModel.setCart(productList)
        }
    }

    /**
     * Every time some changes happens in the dataset, this method get called.
     * Changes could be from server database or user changing the qty of a product
     *
     * @param products: Has the updated product list
     */
    private fun handleProductChanges(owner: LifecycleOwner, products: List<ListItem>) {
        dataset.clear()
        dataset.addAll(products)
        adapter.notifyDataSetChanged()
        updateSummary()
    }

    /**
     * This method gives an overall calculated summary of the selected product
     * This method gets called every time some changes happen in the product list dataset
     */
    private fun updateSummary() {
        val cart = getCart()
        var totalAmount = 0.0
        var totalSelectedQuantity = 0
        val totalSelectedProducts = cart.size

        for (product in cart) {
            totalSelectedQuantity += (product as OSCart).quantity.toInt()
            totalAmount += BusinessItemUtils.getFinalPrice(product.price) * product.quantity
        }

        binding.selectionSummaryInclude.totalAmountTv.text = String.format(Locale.ENGLISH, "Total amount: ₹ %d", totalAmount.toInt())
        binding.selectionSummaryInclude.totalItemTv.text = String.format(Locale.ENGLISH, "Product: %d", totalSelectedProducts)
        binding.selectionSummaryInclude.totalQuantityTv.text = String.format(Locale.ENGLISH, "Qty: %d", totalSelectedQuantity)
    }

    /**
     * Filtered all the selected products from the entire product list
     *
     * @return A list, which has user selected products or empty list
     */
    private fun getCart(): List<ListItem> {
        if (OSListUtils.isEmpty(dataset)) return ArrayList()
        return dataset.filter { i -> (i as OSCart).quantity > 0 }
    }

    /**
     * This method gets called every time a user selected product is no longer available.
     * The unavailable selected products are already removed from the cart when this method gets called.
     */
    private fun handleRemovedCart(owner: LifecycleOwner, removedCart: HashSet<String>) {
        if (removedCart.isEmpty()) return
        AlertDialog.Builder(this).setTitle(getString(R.string.title_product_availability_changed))
                .setMessage(getString(R.string.msg_info_selected_product_status_changed))
                .setCancelable(false).setPositiveButton(getString(R.string.action_btn_ok), null).show()
    }

    private fun onClickedOrder(view: View) {
        val cart = getCart()
        if (OSListUtils.isEmpty(cart)) {
            OSMessage.showSToast(this, getString(R.string.msg_select_product))
            return
        } else if (cart.size > 10) {
            OSMessage.showSToast(this, getString(R.string.msg_max_10_product))
            return
        }
        val intent = Intent(this, OrderSummaryActivity::class.java)
        intent.putExtra(OrderSummaryActivity.CART, Gson().toJson(cart))
        intent.putExtra(OrderSummaryActivity.BUSINESS, bussId)
        startActivity(intent)
    }
}
