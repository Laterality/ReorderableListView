package kr.latera.dndreorderablelistview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by jinwoo on 2016-07-18.
 */
public abstract class ReorderableAdapter<T> extends BaseAdapter
{
	private List<T> mList;
	private static final int INVALID_ID = -1;

	public ReorderableAdapter(@Nullable List<T> list)
	{
		mList = new ArrayList<T>();
		if(list != null) {mList.addAll(list);}
	}

	protected List<T> getList(){return mList;}

	protected void swap(int idx1, int idx2)
	{
		T temp;
		temp = mList.get(idx1);
		mList.set(idx1, mList.get(idx2));
		mList.set(idx2, temp);
		notifyDataSetChanged();
	}
}
