package com.otaliastudios

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import kotlinx.android.synthetic.main.item_icon_with_label.view.*
import net.kibotu.android.recyclerviewpresenter.Adapter
import net.kibotu.android.recyclerviewpresenter.Presenter
import net.kibotu.android.recyclerviewpresenter.PresenterModel

data class Column(val image: String)

class ColumnPresenter :Presenter<Column> (){

    override val layout = R.layout.item_column

    override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder, item: PresenterModel<Column>, position: Int, payloads: MutableList<Any>?, adapter: Adapter) {

        with(viewHolder.itemView) {
            Glide.with(this)
                .asBitmap()
                .load(item.model.image)
                .transition(BitmapTransitionOptions.withCrossFade())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(image)
                .waitForLayout()
                .clearOnDetach()
        }
    }
}