package kr.latera.dndreorderablelistview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity
{

	private ReorderableListView lvList;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		lvList = (ReorderableListView) findViewById(R.id.lv_list);
		lvList.setAdapter(new MyListAdapter(this, Arrays.asList(getResources().getStringArray(R.array.test_entry))));
	}
}
