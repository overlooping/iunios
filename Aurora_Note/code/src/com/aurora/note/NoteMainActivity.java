package com.aurora.note;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import aurora.app.AuroraActivity;
import aurora.app.AuroraActivity.OnSearchViewQuitListener;
import aurora.app.AuroraAlertDialog;
import aurora.widget.AuroraActionBar;
import aurora.widget.AuroraActionBar.OnAuroraActionBarItemClickListener;
import aurora.widget.AuroraActionBarItem;
import aurora.widget.AuroraMenuBase.OnAuroraMenuItemClickListener;
import aurora.widget.AuroraSearchView;
import aurora.widget.floatactionbutton.FloatingActionButton;
import aurora.widget.floatactionbutton.FloatingActionButton.OnFloatActionButtonClickListener;

import com.aurora.note.activity.NewNoteActivity;
import com.aurora.note.activity.manager.NoteManageActivity;
import com.aurora.note.adapter.LabelListAdapter;
import com.aurora.note.adapter.NoteListAdapter2;
import com.aurora.note.alarm.NoteAlarmManager;
import com.aurora.note.alarm.NoteAlarmReceiver;
import com.aurora.note.bean.LabelResult;
import com.aurora.note.bean.NoteResult;
import com.aurora.note.db.LabelAdapter;
import com.aurora.note.db.NoteAdapter;
import com.aurora.note.report.ReportCommand;
import com.aurora.note.report.ReportUtil;
import com.aurora.note.ui.MultiColumnListView;
import com.aurora.note.ui.PLA_AdapterView;
import com.aurora.note.ui.PLA_AbsListView;
import com.aurora.note.util.Globals;
import com.aurora.note.util.Log;
import com.aurora.note.widget.LabelList;

import java.util.ArrayList;

public class NoteMainActivity extends AuroraActivity implements OnSearchViewQuitListener {

	private static final String TAG = "NoteMainActivity";
	private static final int AURORA_NEW_NOTE = 0;
	private static final int AURORA_MANAGER_NOTE = 1;
	private static final int AURORA_SEARCH = 2;

	public static final String EXTRA_KEY_COME_FROM_QUICK_RECORD = "come_from_quick_record";

	private AuroraActionBar mActionBar;
	private AuroraSearchView mSearchView;
	private Button mCancelBtn;
	private View mGotoSearchLayout;
	private LinearLayout mBackgroundSearchLayout;
	private FrameLayout mSearchViewBackground;
	private View mNoMatchView;
	private View mEmptyView;
	// private PullToRefreshListview mListView;
	private MultiColumnListView mListView;
	private LabelList mLabelList;
	//确定删除对话框
	private AuroraAlertDialog mDeleteConDialog;

	private FloatingActionButton mFab;

	private Context mContext;
	private NoteMainActivity mActivity;

	// private long exitTime = 0;

//	private boolean mIsRecreatedInstance;
//	public ImageLoader imageLoader = ImageLoader.getInstance();
	private NoteAdapter mNoteAdapter;
	private LabelAdapter mLabelAdapter;

	private LabelListAdapter mLabelListAdapter;
	// private NoteListAdapter mAdapter;
	private NoteListAdapter2 mAdapter;
	private ArrayList<NoteResult> mListToDisplay = new ArrayList<NoteResult>();
	private ArrayList<NoteResult> mMoreList = null;
	private ArrayList<LabelResult> labelResultList = null;
	//输入的搜索关键字
	private String queryText;
	private boolean isSearchMode = false;
	// 加载更多面板
	private LinearLayout loadMoreView;
	// 底面板加载更多字段控件
	private TextView forum_foot_more;
	private ProgressBar foot_progress;

	// 分页加载滑动标志
	private boolean ifScroll = true;
	// 分页数
	private int pageNum = 1;
	// 一页数据展示数
	private int rowCount = 12;
	private long totalNum = 0;
	// 数据是否加载完毕
	private boolean isLoadDataFinish = false;
	private NoteHandler mNoteHandler = new NoteHandler();

	private boolean mIsComeFromQuickRecord = false; // 是否是从快速录音进来

	private OnAuroraActionBarItemClickListener auroraActionBarItemClickListener = new OnAuroraActionBarItemClickListener() {

		@Override
		public void onAuroraActionBarItemClicked(int itemId) {

			switch (itemId) {
			case AURORA_NEW_NOTE:
				ReportCommand command = new ReportCommand(mContext, ReportUtil.TAG_ADD);
				command.updateData();

				Intent intent = new Intent(NoteMainActivity.this, NewNoteActivity.class);
				startActivityForResult(intent, Globals.REQUEST_CODE_OPEN_NEWNOTE);
				break;
			case AURORA_MANAGER_NOTE:
				Intent intent2 = new Intent(NoteMainActivity.this, NoteManageActivity.class);
				startActivityForResult(intent2, Globals.REQUEST_CODE_OPEN_NEWNOTE);
				break;
			case AURORA_SEARCH:
				onSearchClick();
				break;
			default:
				break;
			}

		}
	};

	private class NoteHandler extends Handler {
		private final int NOTEMAIN_INIT = 0;
		private final int NOTEMAIN_DISVIEW = 1;
		private final int NOTEMAIN_MORE_DISVIEW = 2;
		private final int NOTEMAIN_QUERY = 3;
		private final int NOTEMAIN_SHOW_INPUT = 4;

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case NOTEMAIN_INIT:
				new Thread() {
					@Override
					public void run() {
						/*if (mListToDisplay != null) {
							mListToDisplay.clear();
						}*/
						if (mNoteAdapter == null) return;
						totalNum = mNoteAdapter.getCount();
						mListToDisplay = mNoteAdapter.queryDataByLine(pageNum, rowCount);
						Log.i(TAG, "total Notes::"+totalNum);
						Log.i(TAG, "mListToDisplay is Null::"+(mListToDisplay == null));
						Log.i(TAG, "mListToDisplay::"
								+ (mListToDisplay == null ? 0 : mListToDisplay.size()));
						disview();
					}

				}.start();
				break;
			case NOTEMAIN_DISVIEW:
				updateUIView();
				break;
			case NOTEMAIN_MORE_DISVIEW:
				ifScroll = true;
				if(null ==  mMoreList)
				{
					loadMoreView.setVisibility(View.GONE);
					foot_progress.setVisibility(View.GONE);
					forum_foot_more.setText(R.string.all_loaded);
					break;
				}
				if (mMoreList.size() < rowCount)
					isLoadDataFinish = true;
				for (int i = 0; i < mMoreList.size(); i++) {
					mListToDisplay.add(mMoreList.get(i));
				}
				// mListView.onRefreshComplete();
				mAdapter.notifyDataSetChanged();
				
				if (isLoadDataFinish) {
					loadMoreView.setVisibility(View.GONE);
					foot_progress.setVisibility(View.GONE);
					forum_foot_more.setText(R.string.all_loaded);
				}
				if (mListToDisplay.size() >= totalNum)
				{
					loadMoreView.setVisibility(View.GONE);
				}
		
				break;
			case NOTEMAIN_QUERY:
				new Thread() {
					@Override
					public void run() {
						/*if (mListToDisplay != null) {
							mListToDisplay.clear();
						}*/
						mListToDisplay = mNoteAdapter.queryDataByKey(queryText);
						Log.i(TAG, "mListToDisplay.size()------"
								+ (mListToDisplay == null ? 0 : mListToDisplay.size()));
						disview();
					}

				}.start();
				break;
			case NOTEMAIN_SHOW_INPUT:
				AuroraSearchView searchView = mActionBar.getAuroraActionbarSearchView();
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				if (searchView != null && imm != null){
					imm.showSoftInput(searchView.getQueryTextView(), InputMethodManager.SHOW_FORCED);
				}
				break;
			default:
				super.handleMessage(msg);
				break;
			}
		}

		public void showInput() {
			sendEmptyMessageDelayed(NOTEMAIN_SHOW_INPUT, 200);
		}

		public void initviewdata() {
			sendEmptyMessage(NOTEMAIN_INIT);
		}

		public void disview() {
			sendEmptyMessage(NOTEMAIN_DISVIEW);
		}

		public void dismoreview() {
			sendEmptyMessage(NOTEMAIN_MORE_DISVIEW);
		}

		public void queryKeyNotes() {
			sendEmptyMessage(NOTEMAIN_QUERY);
		}

		public void updateUIView() {
			setAdapter(mListToDisplay);
			if (isSearchMode) {
				if (mListToDisplay == null || mListToDisplay.size() == 0) {
					mNoMatchView.setVisibility(View.VISIBLE);
					mListView.setVisibility(View.GONE);
				} else {
					mNoMatchView.setVisibility(View.GONE);
					mListView.setVisibility(View.VISIBLE);
				}
			} else {
				if (mListToDisplay == null || mListToDisplay.size() == 0) {
					mEmptyView.setVisibility(View.VISIBLE);
					mListView.setVisibility(View.GONE);
				} else {
					mEmptyView.setVisibility(View.GONE);
					mListView.setVisibility(View.VISIBLE);
				}
			}
			updateFootState();
		}

		private void updateFootState() {
			ifScroll = true;
			
			/*mListView
					.onRefreshComplete(getString(R.string.pull_to_refresh_update)
							+ new Date().toLocaleString());*/

			if (isSearchMode) {
				loadMoreView.setVisibility(View.GONE);
				return;
			}

			if (null == mListToDisplay) {
				loadMoreView.setVisibility(View.GONE);
				foot_progress.setVisibility(View.GONE);
				forum_foot_more.setText(R.string.all_loaded);
				return;
			}
			if (totalNum <= rowCount) {
				loadMoreView.setVisibility(View.GONE);
				foot_progress.setVisibility(View.GONE);
				forum_foot_more.setText(R.string.all_loaded);
				return;
			}
			if (isLoadDataFinish) {
				loadMoreView.setVisibility(View.GONE);
				foot_progress.setVisibility(View.GONE);
				forum_foot_more.setText(R.string.all_loaded);
			}
		}

	};

	public void setAdapter(ArrayList<NoteResult> list) {
		// mAdapter = new NoteListAdapter(this, list, queryText);
		// mAdapter.notifyDataSetChanged();
		mAdapter = new NoteListAdapter2(this, list, queryText);
		mListView.setAdapter(mAdapter);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setAuroraContentView(R.layout.note_main_2, AuroraActionBar.Type.Empty);

		mContext = this;
		mActivity = NoteMainActivity.this;

		initDB();
		initViews();
		initActionBar();
		initMenu();
		setListener();
		initdata();
		handleQuickRecord(getIntent());
	}

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "Jim, onNewIntent enter");
        handleQuickRecord(intent);
    }

    private void handleQuickRecord(Intent dataIntent) {
        if (dataIntent.getBooleanExtra(EXTRA_KEY_COME_FROM_QUICK_RECORD, false)) {
            mIsComeFromQuickRecord = true;
            Intent intent = new Intent(NoteMainActivity.this, NewNoteActivity.class);
            intent.putExtra(NewNoteActivity.EXTRA_KEY_COME_FROM_QUICK_RECORD, true);
            startActivityForResult(intent, Globals.REQUEST_CODE_OPEN_NEWNOTE);
        } else {
            mIsComeFromQuickRecord = false;
        }
    }

    private void initdata() {
        NoteAlarmReceiver.initNoteData(this);

    	isLoadDataFinish = false;
		pageNum = 1;
    	queryText = "";
    	mNoteHandler.initviewdata();
	}

	private void initDB() {
		mNoteAdapter = new NoteAdapter(this);
		mLabelAdapter = new LabelAdapter(this);
		mNoteAdapter.open();
		mLabelAdapter.open();
	}

	private void initActionBar() {
		mActionBar = getAuroraActionBar();
		mActionBar.setTitle(R.string.app_name);

		// mActionBar.getTitleView().setTextColor(getResources().getColor(R.color.note_main_text_color));
		// mActionBar.setBackgroundResource(R.color.note_main_actionbar_bg_color);

		// addAuroraActionBarItem(AuroraActionBarItem.Type.Add, AURORA_NEW_NOTE);
		// mActionBar.addItem(R.drawable.note_main_new_selector, AURORA_NEW_NOTE, getString(R.string.new_note));
		// mActionBar.addItem(R.drawable.note_main_setting_selector, AURORA_MANAGER_NOTE, getString(R.string.action_settings));

		mActionBar.addItem(AuroraActionBarItem.Type.Search, AURORA_SEARCH);
		mActionBar.addItem(AuroraActionBarItem.Type.Set, AURORA_MANAGER_NOTE);
		mActionBar.setOnAuroraActionBarListener(auroraActionBarItemClickListener);

		mFab = (FloatingActionButton) findViewById(R.id.new_note);
		mFab.setOnFloatingActionButtonClickListener(new OnFloatActionButtonClickListener() {
			@Override
			public void onClick() {
				ReportCommand command = new ReportCommand(mContext, ReportUtil.TAG_ADD);
				command.updateData();

				Intent intent = new Intent(NoteMainActivity.this, NewNoteActivity.class);
				startActivityForResult(intent, Globals.REQUEST_CODE_OPEN_NEWNOTE);
			}
		});
	}

	private void initMenu() {
		setAuroraSystemMenuCallBack(new OnAuroraMenuItemClickListener() {
			@Override
			public void auroraMenuItemClick(int itemId) {
				switch (itemId) {
				case R.id.action_note_manage:
					Intent intent2 = new Intent(NoteMainActivity.this, NoteManageActivity.class);
					startActivityForResult(intent2, Globals.REQUEST_CODE_OPEN_NEWNOTE);
					break;
				default:
					break;
				}
			}
		});

		setAuroraMenuItems(R.menu.note_main_menu);
	}

	private void initViews() {
		// addSearchviewInwindowLayout();

		mEmptyView = findViewById(R.id.no_note_fra);
		// mListView = (PullToRefreshListview)getListView();
		mListView = (MultiColumnListView) findViewById(R.id.note_list_view);
		mListView.setDivider(null);
		mListView.setSelector(R.drawable.note_list_item_top_selector);
		mListView.setDrawSelectorOnTop(true);

		Resources resources = getResources();
		mListView.setSelectorInnerPadding(
				resources.getDimensionPixelOffset(R.dimen.note_list2_padding_left),
				resources.getDimensionPixelOffset(R.dimen.note_list2_padding_top),
				resources.getDimensionPixelOffset(R.dimen.note_list2_padding_left), 0);
		/*mListView.setColumnPadding(
				resources.getDimensionPixelOffset(R.dimen.note_list2_padding), 
				resources.getDimensionPixelOffset(R.dimen.note_list2_padding));*/

		mNoMatchView = findViewById(R.id.no_match);
		// mSearchViewBackground = (FrameLayout) getSearchViewGreyBackground();
		mSearchViewBackground = (FrameLayout) findViewById(R.id.search_view_background);
		mBackgroundSearchLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.search_view_background, null, false);

		loadMoreView = (LinearLayout) getLayoutInflater().inflate(R.layout.listview_footer, null);
		loadMoreView.setClickable(false);
		loadMoreView.setLongClickable(false);
		forum_foot_more = (TextView) loadMoreView.findViewById(R.id.listview_foot_more);
		foot_progress = (ProgressBar) loadMoreView.findViewById(R.id.listview_foot_progress);

		mListView.addFooterView(loadMoreView);

		if (mSearchViewBackground != null) {
			mSearchViewBackground.addView(mBackgroundSearchLayout);
			mLabelList = (LabelList) mBackgroundSearchLayout.findViewById(R.id.label_list);
		}
	}

	public void initLabelAdapter() {
		if (mSearchViewBackground == null) return;

		if (labelResultList != null) {
			labelResultList.clear();
			labelResultList = null;
		}

		labelResultList = mLabelAdapter.queryAllData();
		if (null != labelResultList) {
			mLabelListAdapter = new LabelListAdapter(mContext, labelResultList, mActivity);
			mLabelList.setAdapter(mLabelListAdapter);
		}

		updateLabelVisibitity();
	}

	private void updateLabelVisibitity() {
		if (labelResultList != null && !labelResultList.isEmpty()) {
			mSearchViewBackground.setVisibility(View.VISIBLE);
		} else {
			mSearchViewBackground.setVisibility(View.GONE);
		}
	}

	private void onSearchClick() {
		AuroraSearchView searchView = mActionBar.getAuroraActionbarSearchView();
		if (searchView == null) {
			return;
		}
		ImageButton backSearchBtn = mActionBar.getAuroraActionbarSearchViewBackButton();
		if (backSearchBtn != null) {
			backSearchBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					hideSearchViewLayout(true);
				}
			});
		}
		initLabelAdapter();
		searchView.setOnQueryTextListener(new searchViewQueryTextListener());
		showSearchviewLayout();
		setSearchMode(true);

		mNoteHandler.showInput();
	}

	private void setListener() {
		if (mSearchViewBackground != null) {
			mSearchViewBackground.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					hideSearchViewLayout(true);
				}
			});
		}

		mGotoSearchLayout = mListView.getHeadView();
		if (mGotoSearchLayout != null) {
			mGotoSearchLayout.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					mSearchView = getSearchView();
					mCancelBtn = getSearchViewRightButton();
					if (null == mSearchView) {
						return;
					}
					if (mCancelBtn != null) {
						mCancelBtn.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								hideSearchViewLayout(true);
							}

						});
					}
					initLabelAdapter();
					setOnQueryTextListener(new svQueryTextListener());
					showSearchviewLayout();
					setSearchMode(true);
				}
			});
		}

		mListView.setOnItemClickListener(new PLA_AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(PLA_AdapterView<?> parent, View view, int position, long id) {
				closeSoftInputWindow();

				Bundle bl = new Bundle();
				bl.putInt(NewNoteActivity.TYPE_GET_DATA, 0);
				bl.putParcelable(NewNoteActivity.NOTE_OBJ, (NoteResult)mListView.getAdapter().getItem(position));

				Intent intent = new Intent(NoteMainActivity.this, NewNoteActivity.class);
				intent.putExtras(bl);
				startActivityForResult(intent, Globals.REQUEST_CODE_OPEN_NEWNOTE);
			}

		});

		mListView.setLongClickable(true);
		mListView.setOnItemLongClickListener(new PLA_AdapterView.OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(PLA_AdapterView<?> parent, View view, final int position, long id) {
				if (isSearchMode) return false;

				view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

				mDeleteConDialog = new AuroraAlertDialog.Builder(mContext)
                        .setTitle(R.string.dialog_delete_note_title)
                        //.setMessage(R.string.dialog_delete_note_message)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.delete,
                        		new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog, int which) {
										deleteListItem(position);
									}

                        		}).create();
				mDeleteConDialog.show();
				return true;
			}

		});

		mListView.setOnScrollListener(new PLA_AbsListView.OnScrollListener() {

			@Override
			public void onScrollStateChanged(PLA_AbsListView view, int scrollState) {
				// mListView.onScrollStateChanged(view, scrollState);

				if (isLoadDataFinish || isSearchMode) return;

				boolean scrollEnd = false;
				try {
					if (view.getPositionForView(loadMoreView) == view.getLastVisiblePosition()) {
						scrollEnd = true;
					}
				} catch (Exception e) {
					scrollEnd = false;
				}

				if(mListToDisplay == null || mListToDisplay.size() < pageNum * rowCount) return;

				if (scrollEnd) {
					loadMoreView.setVisibility(View.GONE);
					foot_progress.setVisibility(View.GONE);
					forum_foot_more.setText(R.string.loading);

					new Thread() {
						public void run() {
							loadMoreData();
							mNoteHandler.dismoreview();
						}
					}.start();
				}
			}

			@Override
			public void onScroll(PLA_AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				// mListView.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
			}
		});

		/*mListView.setOnRefreshListener(new PLA_PullListview.OnRefreshListener() {
			@Override
			public void onRefresh() {
				mListView.onRefreshComplete(getString(R.string.pull_to_refresh_update) + new Date().toString());
			}
		});*/

		setOnSearchViewQuitListener(this);
	}

	private void deleteListItem(int position) {
		NoteResult deleteNote = (NoteResult) mListView.getAdapter().getItem(position);
		mNoteAdapter.deleteDataById(String.valueOf(deleteNote.getId()));
		NoteAlarmReceiver.scheduleAlarmById(deleteNote.getId(), NoteAlarmManager.ACTION_DELETE);

		totalNum = mNoteAdapter.getCount();
		//删除mListToDisplay对应的数据
		mListToDisplay.remove(position - mListView.getHeaderViewsCount());
		if (mListToDisplay.size() >= totalNum) {
			// mAdapter.notifyDataSetChanged();
			setAdapter(mListToDisplay);
		} else {
			NoteResult addNote = mNoteAdapter.queryDataByIndex(pageNum, rowCount);
			if (null == addNote) {
				mAdapter.notifyDataSetChanged();
				return;
			}
			mListToDisplay.add(addNote);
			mAdapter.notifyDataSetChanged();

			if (mListToDisplay.size() >= totalNum) {
				loadMoreView.setVisibility(View.GONE);
			}
		}
		if (totalNum == 0) {
			mEmptyView.setVisibility(View.VISIBLE);
			mListView.setVisibility(View.GONE);
		} else {
			mEmptyView.setVisibility(View.GONE);
			mListView.setVisibility(View.VISIBLE);
		}
	}

	private void closeSoftInputWindow() {
		if (getCurrentFocus() != null) {
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
		}
	}

	private synchronized void loadMoreData() {
		
			if (ifScroll) {
				ifScroll=false;

				pageNum++;
				mMoreList = new ArrayList<NoteResult>();
				
				// aurora ukiliu 2014-05-09 modify for BUG #4742 begin
				if (null != mNoteAdapter) {
					mMoreList = mNoteAdapter.queryDataByLine(pageNum, rowCount);
				}
				// aurora ukiliu 2014-05-09 modify for BUG #4742 end
				
			}
	}

	@Override
	public boolean quit() {
		mActionBar.getAuroraActionbarSearchView().clearText();
		//退出搜索模式的时候需要放出mListView的搜索头部
		if (mGotoSearchLayout != null) {
			mGotoSearchLayout.setVisibility(View.VISIBLE);
		}
		if (mSearchViewBackground != null) {
			mSearchViewBackground.setVisibility(View.GONE);
		}
		setSearchMode(false);
		// mListView.auroraSetNeedSlideDelete(true);
		initdata();
		return false;
	}

	@Override
	public void finish() {
		super.finish();
		if (null != mNoteAdapter) {
			mNoteAdapter.close();
			mNoteAdapter = null;
		}
		if (null != mLabelAdapter) {
			mLabelAdapter.close();
			mLabelAdapter = null;
		}
	}

	@Override
	protected void onResume() {
		// mListView.auroraOnResume();
		super.onResume();
	}

	@Override
	protected void onPause() {
		// mListView.auroraOnPause();
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Globals.REQUEST_CODE_OPEN_NEWNOTE:
                if (resultCode == Activity.RESULT_OK) {
                    if (!isSearchMode) {
                        initdata();
                    } else {
                        mNoteHandler.queryKeyNotes();
                        // mListView.auroraSetNeedSlideDelete(false);
                        // mAdapter.setAuroraListHasDelete(false);
                    }
                    mIsComeFromQuickRecord = false;
                } else {
                    if (mIsComeFromQuickRecord) {
                        finish();
                    }
                }
                break;
            case Globals.REQUEST_CODE_MODIFY_LABEL:
                if (resultCode == Activity.RESULT_OK) {
                    initLabelAdapter();
                }
                break;
            default:
                break;
        }
	}

	private final class searchViewQueryTextListener implements AuroraSearchView.OnQueryTextListener {
		@Override
		public boolean onQueryTextChange(String queryText) {
			if (queryText != null) {
				queryText = queryText.replace("_", "''").replace("%", "''");
			}

			if (queryText.length() > 0) {
				setQueryText(queryText);
				mListView.setVisibility(View.GONE);
				mNoteHandler.queryKeyNotes();

				if (mSearchViewBackground != null) {
					mSearchViewBackground.setVisibility(View.GONE);
				}
			} else {
				updateLabelVisibitity();
				if (labelResultList == null || labelResultList.isEmpty()) {
					initdata();
				}
			}

			return false;
		}

		@Override
		public boolean onQueryTextSubmit(String arg0) {
			return false;
		}
	}

	// 搜索输入响应监听
	private final class svQueryTextListener implements OnSearchViewQueryTextChangeListener {

		@Override
		public boolean onQueryTextChange(String queryText) {
			// do my thing
			Log.i(TAG, "mQueryText===============" + queryText);

			if (queryText != null) {
				queryText = queryText.replace("_", "''").replace("%", "''");
			}

			if (queryText.length() > 0) {
				setQueryText(queryText);
				mListView.setVisibility(View.GONE);
				mNoteHandler.queryKeyNotes();

				//以下为：搜索结果大于一页时，防止拖拽出mListView的搜索头部
				if (mGotoSearchLayout != null) {
					mGotoSearchLayout.setVisibility(View.GONE);
				}
			} else {
				// mListView.setVisibility(View.GONE);
				initdata();
			}
			// mAdapter.setAuroraListHasDelete(false);

			return false;
		}

		@Override
		public boolean onQueryTextSubmit(String arg0) {
			return false;
		}

	}

	public void setQueryText(String queryText) {
		this.queryText = queryText;
	}

	//判断当前主界面是否为搜索模式
	public boolean isSearchMode() {
		return isSearchMode;
	}

	//设置当前主界面的搜索模式
	public void setSearchMode(boolean isSearchMode) {
		this.isSearchMode = isSearchMode;
		mFab.setVisibility(isSearchMode ? View.GONE : View.VISIBLE);
		/*if (mListView != null) {
			mListView.setCanMoveHeadView(!isSearchMode);
			mListView.showHeadView();
		}*/
	}

	/*public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK: {
				boolean deleteIsShow = mListView.auroraIsRubbishOut();
				if (deleteIsShow) {
					mListView.auroraSetRubbishBack();
					return true;
				}

				if (isSearchMode) break;

				if (System.currentTimeMillis() - exitTime > 3000) {
					ToastUtil.shortToast(R.string.click_again_to_exit);
					exitTime = System.currentTimeMillis();
					return true;
				}

				gotoHome();
				return true;
			}
			default: {
				
			}
		}

		return super.onKeyDown(keyCode, event);
	}

	private void gotoHome() {
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}*/

}