package kr.latera.reorderablelistview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * Created by jinwoo on 2016-07-18.
 */
public class ReorderableListView extends ListView
{

	private static final String TAG = "ReorderableListview";

	private final int SMOOTH_SCROLL_AMOUNT_AT_EDGE = 15;
	private final int MOVE_DURATION = 150;
	private final int LINE_THICKNESS = 5;

	private int mLastEventY = -1;

	private int mDownY = -1;
	private int mDownX = -1;

	private int mTotalOffset = 0;

	private boolean mCellIsMobile = false;
	private boolean mIsMobileScrolling = false;
	private int mSmoothScrollAmountAtEdge = 0;

	private final int INVALID_ID = -1;
	private long mAboveItemId = INVALID_ID;
	private long mMobileItemId = INVALID_ID;
	private long mBelowItemId = INVALID_ID;

	private BitmapDrawable mHoverCell;
	private Rect mHoverCellCurrentBounds;
	private Rect mHoverCellOriginalBounds;

	private final int INVALID_POINTER_ID = -1;
	private int mActivePointerId = INVALID_POINTER_ID;

	private boolean mIsWaitingForScrollFinish = false;
	private int mScrollState = OnScrollListener.SCROLL_STATE_IDLE;

	//
	// BEGIN : init codes
	//

	public ReorderableListView(Context context) {
		super(context);
		init(context);
	}

	public ReorderableListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public ReorderableListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public void init(Context context) {
		setOnItemLongClickListener(mOnItemLongClickListener);
		setOnScrollListener(mScrollListener);
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		mSmoothScrollAmountAtEdge = (int) (SMOOTH_SCROLL_AMOUNT_AT_EDGE / metrics.density);
	}

	//
	// END : init codes
	//


	//
	// BEGIN : make hover cell of selected view
	//

	private OnItemLongClickListener mOnItemLongClickListener = new OnItemLongClickListener() {
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) {
			mTotalOffset = 0;

			int position = pointToPosition(mDownX, mDownY);
			int itemNum = position - getFirstVisiblePosition();

			View selectedView = getChildAt(itemNum);
			mMobileItemId = getAdapter().getItemId(position);
			mHoverCell = getAndAddHoverView(selectedView);
			selectedView.setVisibility(INVISIBLE);

			mCellIsMobile = true;

			updateNeighborViewsForID(mMobileItemId);

			return true;
		}
	};

	private BitmapDrawable getAndAddHoverView(View v) {

		int w = v.getWidth();
		int h = v.getHeight();
		int top = v.getTop();
		int left = v.getLeft();

		Bitmap b = getBitmapWithBorder(v);

		BitmapDrawable drawable = new BitmapDrawable(getResources(), b);

		mHoverCellOriginalBounds = new Rect(left, top, left + w, top + h);
		mHoverCellCurrentBounds = new Rect(mHoverCellOriginalBounds);

		drawable.setBounds(mHoverCellCurrentBounds);

		return drawable;
	}


	/**
	 *
	 * @param v
	 * @return border line of hover cell
	 */
	private Bitmap getBitmapWithBorder(View v) {
		Bitmap bitmap = getBitmapFromView(v);
		Canvas can = new Canvas(bitmap);

		Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(LINE_THICKNESS);
		paint.setColor(Color.BLACK);

		can.drawBitmap(bitmap, 0, 0, null);
		can.drawRect(rect, paint);

		return bitmap;
	}

	/**
	 *
	 * @param v
	 * @return get drawable shape of @param v
	 */
	private Bitmap getBitmapFromView(View v) {
		Bitmap bitmap = Bitmap.createBitmap(v.getWidth(), v.getHeight(),
				Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		v.draw(canvas);
		return bitmap;
	}

	//
	// END : make hover cell of selected view
	//

	//
	// BEGIN : manage item id of each item
	//

	private void updateNeighborViewsForID(long itemID) {
		int position = getPositionForID(itemID);
		ReorderableAdapter adapter = getAdapter();
		if(!(position - 1 < 0)) {mAboveItemId = adapter.getItemId(position - 1);}
		else {mAboveItemId = INVALID_ID;}
		if(!(position + 1 >= getAdapter().getCount())) {mBelowItemId = adapter.getItemId(position + 1);}
		else {mBelowItemId = INVALID_ID;}
	}

	public View getViewForID(long itemID) {
		int firstVisiblePosition = getFirstVisiblePosition();
		ReorderableAdapter adapter = getAdapter();
		for (int i = 0; i < getChildCount(); i++) {
			View v = getChildAt(i);
			int position = firstVisiblePosition + i;
			long id = adapter.getItemId(position);
			if (id == itemID) {
				return v;
			}
		}
		return null;
	}

	public int getPositionForID(long itemID) {
		View v = getViewForID(itemID);
		if (v == null) {
			return -1;
		} else {
			return getPositionForView(v);
		}
	}

	//
	// BEGIN : manage item id of each item
	//

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		if (mHoverCell != null) {
			mHoverCell.draw(canvas);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				mDownX = (int) event.getX();
				mDownY = (int) event.getY();
				mActivePointerId = event.getPointerId(0);
				break;
			case MotionEvent.ACTION_MOVE:
				if (mActivePointerId == INVALID_POINTER_ID) {
					break;
				}

				int pointerIndex = event.findPointerIndex(mActivePointerId);

				mLastEventY = (int) event.getY(pointerIndex);
				int deltaY = mLastEventY - mDownY;

				if (mCellIsMobile) {
					mHoverCellCurrentBounds.offsetTo(mHoverCellOriginalBounds.left,
							mHoverCellOriginalBounds.top + deltaY + mTotalOffset);
					mHoverCell.setBounds(mHoverCellCurrentBounds);
					invalidate();

					handleCellSwitch();

					mIsMobileScrolling = false;
					handleMobileCellScroll();

					return false;
				}
				break;
			case MotionEvent.ACTION_UP:
				touchEventsEnded();
				break;
			case MotionEvent.ACTION_CANCEL:
				touchEventsCancelled();
				break;
			case MotionEvent.ACTION_POINTER_UP:
				pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
				final int pointerId = event.getPointerId(pointerIndex);
				if (pointerId == mActivePointerId) {
					touchEventsEnded();
				}
				break;
			default:
				break;
		}

		return super.onTouchEvent(event);
	}

	//
	// BEGIN : handle switching cell
	//

	/**
	 * judging if the hover cell has been shifted far enough to swap two cells.
	 * If so, invoke cell swap through the ReorderableAdapter#swap
	 */
	private void handleCellSwitch() {
		final int deltaY = mLastEventY - mDownY;
		int deltaYTotal = mHoverCellOriginalBounds.top + mTotalOffset + deltaY;

		View belowView = getViewForID(mBelowItemId);
		View mobileView = getViewForID(mMobileItemId);
		View aboveView = getViewForID(mAboveItemId);

		boolean isBelow = (belowView != null)
				&& (deltaYTotal > belowView.getTop());
		boolean isAbove = (aboveView != null)
				&& (deltaYTotal < aboveView.getTop());

		if (isBelow || isAbove) {

			final long switchItemID = isBelow ? mBelowItemId : mAboveItemId;
			View switchView = isBelow ? belowView : aboveView;
			final int originalItem = getPositionForView(mobileView);

			if (switchView == null) {
				updateNeighborViewsForID(mMobileItemId);
				return;
			}

			getAdapter().swap(originalItem, getPositionForView(switchView));

			getAdapter().notifyDataSetChanged();

			mDownY = mLastEventY;

			final int switchViewStartTop = switchView.getTop();

			mobileView.setVisibility(View.VISIBLE);
			switchView.setVisibility(View.VISIBLE);

			updateNeighborViewsForID(mMobileItemId);

			final ViewTreeObserver observer = getViewTreeObserver();
			observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
				@SuppressLint("NewApi")
				public boolean onPreDraw() {
					observer.removeOnPreDrawListener(this);

					View switchView = getViewForID(switchItemID);

					mTotalOffset += deltaY;

					int switchViewNewTop = switchView.getTop();
					int delta = switchViewStartTop - switchViewNewTop;

					switchView.setTranslationY(delta);

					ObjectAnimator animator = ObjectAnimator.ofFloat(
							switchView, View.TRANSLATION_Y, 0);
					animator.setDuration(MOVE_DURATION);
					animator.start();

					return true;
				}
			});
		}
	}

	private void touchEventsEnded() {
		final View mobileView = getViewForID(mMobileItemId);
		if (mCellIsMobile || mIsWaitingForScrollFinish) {
			mCellIsMobile = false;
			mIsWaitingForScrollFinish = false;
			mIsMobileScrolling = false;
			mActivePointerId = INVALID_POINTER_ID;

			if (mScrollState != OnScrollListener.SCROLL_STATE_IDLE) {
				mIsWaitingForScrollFinish = true;
				return;
			}

			mHoverCellCurrentBounds.offsetTo(mHoverCellOriginalBounds.left,
					mobileView.getTop());

			ObjectAnimator hoverViewAnimator = ObjectAnimator.ofObject(
					mHoverCell, "bounds", sBoundEvaluator,
					mHoverCellCurrentBounds);
			hoverViewAnimator
					.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
						@Override
						public void onAnimationUpdate(
								ValueAnimator valueAnimator) {
							invalidate();
						}
					});
			hoverViewAnimator.addListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationStart(Animator animation) {
					setEnabled(false);
				}

				@Override
				public void onAnimationEnd(Animator animation) {
					mAboveItemId = INVALID_ID;
					mMobileItemId = INVALID_ID;
					mBelowItemId = INVALID_ID;
					mobileView.setVisibility(VISIBLE);
					mHoverCell = null;
					setEnabled(true);
					invalidate();
				}
			});
			hoverViewAnimator.start();
		} else {
			touchEventsCancelled();
		}
		Log.d(TAG, "touchEventsEnded...end point");
	}

	private void touchEventsCancelled() {
		View mobileView = getViewForID(mMobileItemId);
		if (mCellIsMobile) {
			mAboveItemId = INVALID_ID;
			mMobileItemId = INVALID_ID;
			mBelowItemId = INVALID_ID;
			mobileView.setVisibility(VISIBLE);
			mHoverCell = null;
			invalidate();
		}
		mCellIsMobile = false;
		mIsMobileScrolling = false;
		mActivePointerId = INVALID_POINTER_ID;
	}

	private final static TypeEvaluator sBoundEvaluator = new TypeEvaluator<Rect>() {
		@Override
		public Rect evaluate(float fraction, Rect startValue, Rect endValue) {
			return new Rect(interpolate(startValue.left, endValue.left,
					fraction), interpolate(startValue.top, endValue.top,
					fraction), interpolate(startValue.right, endValue.right,
					fraction), interpolate(startValue.bottom, endValue.bottom,
					fraction));
		}

		public int interpolate(int start, int end, float fraction) {
			return (int) (start + fraction * (end - start));
		}
	};

	private void handleMobileCellScroll() {
		mIsMobileScrolling = handleMobileCellScroll(mHoverCellCurrentBounds);
	}

	public boolean handleMobileCellScroll(Rect r) {
		int offset = computeVerticalScrollOffset();
		int height = getHeight();
		int extent = computeVerticalScrollExtent();
		int range = computeVerticalScrollRange();
		int hoverViewTop = r.top;
		int hoverHeight = r.height();

		if (hoverViewTop <= 0 && offset > 0) {
			smoothScrollBy(-mSmoothScrollAmountAtEdge, 0);
			return true;
		}

		if (hoverViewTop + hoverHeight >= height && (offset + extent) < range) {
			smoothScrollBy(mSmoothScrollAmountAtEdge, 0);
			return true;
		}

		return false;
	}

	private OnScrollListener mScrollListener = new OnScrollListener() {

		private int mPreviousFirstVisibleItem = -1;
		private int mPreviousVisibleItemCount = -1;
		private int mCurrentFirstVisibleItem;
		private int mCurrentVisibleItemCount;
		private int mCurrentScrollState;

		public void onScroll(AbsListView view, int firstVisibleItem,
		                     int visibleItemCount, int totalItemCount) {
			mCurrentFirstVisibleItem = firstVisibleItem;
			mCurrentVisibleItemCount = visibleItemCount;

			mPreviousFirstVisibleItem = (mPreviousFirstVisibleItem == -1) ? mCurrentFirstVisibleItem
					: mPreviousFirstVisibleItem;
			mPreviousVisibleItemCount = (mPreviousVisibleItemCount == -1) ? mCurrentVisibleItemCount
					: mPreviousVisibleItemCount;

			checkAndHandleFirstVisibleCellChange();
			checkAndHandleLastVisibleCellChange();

			mPreviousFirstVisibleItem = mCurrentFirstVisibleItem;
			mPreviousVisibleItemCount = mCurrentVisibleItemCount;
		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			mCurrentScrollState = scrollState;
			mScrollState = scrollState;
			isScrollCompleted();
		}

		private void isScrollCompleted() {
			if (mCurrentVisibleItemCount > 0
					&& mCurrentScrollState == SCROLL_STATE_IDLE) {
				if (mCellIsMobile && mIsMobileScrolling) {
					handleMobileCellScroll();
				} else if (mIsWaitingForScrollFinish) {
					touchEventsEnded();
				}
			}
		}

		public void checkAndHandleFirstVisibleCellChange() {
			if (mCurrentFirstVisibleItem != mPreviousFirstVisibleItem) {
				if (mCellIsMobile && mMobileItemId != INVALID_ID) {
					updateNeighborViewsForID(mMobileItemId);
					handleCellSwitch();
				}
			}
		}

		public void checkAndHandleLastVisibleCellChange() {
			int currentLastVisibleItem = mCurrentFirstVisibleItem
					+ mCurrentVisibleItemCount;
			int previousLastVisibleItem = mPreviousFirstVisibleItem
					+ mPreviousVisibleItemCount;
			if (currentLastVisibleItem != previousLastVisibleItem) {
				if (mCellIsMobile && mMobileItemId != INVALID_ID) {
					updateNeighborViewsForID(mMobileItemId);
					handleCellSwitch();
				}
			}
		}
	};

	@Override
	public ReorderableAdapter getAdapter() {return (ReorderableAdapter) super.getAdapter();}

	@Deprecated
	public void setAdapter(ListAdapter adapter)
	{
		try {throw new Exception("call #setAdapter(ReorderableAdapter) instead");}
		catch (Exception e) {}
	}
	public void setAdapter(ReorderableAdapter adapter)
	{
		super.setAdapter(adapter);
	}


}
