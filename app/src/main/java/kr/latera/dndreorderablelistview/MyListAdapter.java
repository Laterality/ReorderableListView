package kr.latera.dndreorderablelistview;

import android.content.Context;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jinwoo on 2016-07-18.
 */
public class MyListAdapter extends ReorderableAdapter<String>
{

	private Context mContext;

	public MyListAdapter(Context context, @Nullable List<String> list)
	{
		super(list);
		mContext = context;
	}

	@Override
	public int getCount()
	{
		return getList().size();
	}

	@Override
	public String getItem(int i)
	{
		return getList().get(i);
	}

	@Override
	public long getItemId(int i)
	{
		return getList().get(i).hashCode();
	}

	@Override
	public View getView(int i, View view, ViewGroup viewGroup)
	{
		ViewHolder holder;
		String item = getList().get(i);

		if(view == null)
		{
			holder = new ViewHolder();
			view = LayoutInflater.from(mContext).inflate(R.layout.item_list, null);

			holder.tvText = (TextView) view.findViewById(R.id.tv_item_list);

			view.setTag(holder);
		}
		else
		{
			holder = (ViewHolder)view.getTag();
		}

		holder.tvText.setText(item);


		return view;
	}

	private class ViewHolder
	{
		TextView tvText;
	}
}
