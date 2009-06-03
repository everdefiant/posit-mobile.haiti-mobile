/*******************************************************************************
 * Copyright (c) 2009 .
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     pgautam - initial API and implementation
 ******************************************************************************/
package org.hfoss.posit;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.SimpleCursorAdapter.ViewBinder;

public class ListFindsActivity extends ListActivity implements ViewBinder{

	private static final String TAG = "ListActivity";
	private MyDBHelper mDbHelper;
	private Cursor mCursor;  // Used for DB accesses

	/** 
	 * This method is invoked when the Activity starts and
	 *  when the user navigates back to ListFindsActivity
	 *  from some other app. It creates a
	 *  DBHelper and calls fillData() to fetch data from the DB.
	 *  @param savedInstanceState contains the Activity's previously
	 *   frozen state.  In this case it is unused.
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		setContentView(R.layout.list_finds);
		mDbHelper = new MyDBHelper(this);
		fillData();
		mDbHelper.close();
	}

	/** 
	 * This method is called when the activity is ready to start 
	 *  interacting with the user. It is at the top of the Activity
	 *  stack.
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		fillData();
	}

	/**
	 * Called when the system is about to resume some other activity.
	 *  It can be used to save state, if necessary.  In this case
	 *  we close the cursor to the DB to prevent memory leaks.
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onPause(){
		super.onPause();
        stopManagingCursor(mCursor);
        mDbHelper.close();
        mCursor.close();
//		mDbHelper.close();  // NOTE WELL: Can't close while managing cursor
	}

	
	@Override
	protected void onStop() {
		super.onStop();
		mDbHelper.close();
		mCursor.close();
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
		mDbHelper.close();
		mCursor.close();
	}


	/**
	 * Puts the items from the DB table into the rows of the view. Note that
	 *  once you start managing a Cursor, you cannot close the DB without 
	 *  causing an error.
	 */
	private void fillData() {
		Log.i(TAG, "filldata: refilling the data");

		String[] columns = MyDBHelper.list_row_data;
		int [] views = MyDBHelper.list_row_views;
		
		mCursor = mDbHelper.fetchAllFinds();		
    	if (mCursor.getCount() == 0) { // No finds
    		setContentView(R.layout.list_finds);
    		return;
    	}
    	startManagingCursor(mCursor); // NOTE: Can't close DB while managing cursor

        // CursorAdapter binds the data in 'columns' to the views in 'views' 
        SimpleCursorAdapter adapter = 
        		new SimpleCursorAdapter(this, R.layout.list_row, mCursor, columns, views);
        adapter.setViewBinder(this);
        setListAdapter(adapter); 
		Log.i(TAG, "filldata: refilled the data");
	}
	

	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	/**
	 * This method executes when the user clicks on one of the Finds in the
	 *   list. It starts the FindActivity in EDIT mode, which will read
	 *   the Find's data from the DB.
	 *   @param l is the ListView that was clicked on 
	 *   @param v is the View within the ListView
	 *   @param position is the View's position in the ListView
	 *   @param id is the Find's RowID
	 */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent intent = new Intent(this, FindActivity.class);
        intent.setAction(Intent.ACTION_EDIT);
        intent.putExtra(MyDBHelper.KEY_ID, id); // Pass the RowID to FindActivity
        startActivity(intent);
    }
	
	/**
	 * This method creates the menus for this activity.
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.list_finds_menu, menu);
		return true;
	}

	/** 
	 * This method is invoked when a menu item is selected. It starts
	 *   the appropriate Activity.
	 * @see android.app.Activity#onMenuItemSelected(int, android.view.MenuItem)
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {
			case R.id.new_find_menu_item:
				intent = new Intent (this, FindActivity.class);
				intent.setAction(Intent.ACTION_INSERT);
				startActivity(intent);
				break;
			case R.id.sync_finds_menu_item: 
			    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		        boolean syncIsOn = sp.getBoolean("SYNC_ON_OFF", true);
		        if (!syncIsOn) {
					Utils.showToast(this, "Synchronization is turned off.");
					break;
		        }
				intent = new Intent(this, SyncActivity.class);
				intent.setAction(Intent.ACTION_SYNC);
				startActivity(intent);
				break;
			case R.id.map_finds_menu_item:
				mDbHelper.close();
				intent = new Intent(this, MapFindsActivity.class);
				startActivity(intent);
				break;
			case R.id.delete_finds_menu_item:
				mDbHelper.close();
				intent = new Intent(this, FindActivity.class);
				intent.setAction(this.getString(R.string.delete_finds));
				startActivity(intent);
				break;
		}
		return true;
	}

	/**
	 * Binds to the view Binder to show the image and if the image is synced.
	 * @author pgautam
	 *
	 */
	public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
		switch (view.getId()) {
		case R.id.find_image:
			int rowId = cursor.getInt(cursor
					.getColumnIndexOrThrow(MyDBHelper.KEY_ID));
			Cursor imagesQuery = managedQuery(
					MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 
					new String[] {
					BaseColumns._ID, ImageColumns.BUCKET_ID },
					ImageColumns.BUCKET_ID + "=\"347330322\"", // AND "
							//+ ImageColumns.BUCKET_DISPLAY_NAME + "=\"posit|"
							//+ rowId + "\"", 
							null, null);
			/**
			Log.i(TAG, "cusor count = " + imagesQuery.getCount());
			imagesQuery.moveToFirst();
			 for (int k = 0; k < imagesQuery.getCount(); k++) {
		     for (String column : imagesQuery.getColumnNames()) {
		    	 Log.i(TAG, column + "=" +  imagesQuery.getString(imagesQuery.getColumnIndexOrThrow(column)));
		     }
		     imagesQuery.moveToNext();
			 }

**/
			try {
				if (imagesQuery.getCount() > 0) {
					imagesQuery.moveToFirst();
					ImageView i = (ImageView) view;
					int id = imagesQuery.getInt(cursor
							.getColumnIndexOrThrow(BaseColumns._ID));
					i.setImageURI(Uri.withAppendedPath(
							MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
							"" + id));
					i.setScaleType(ImageView.ScaleType.FIT_XY);
				}
			} catch (NullPointerException e) {
				// avoid null imageQueries
			}
			return true;
			
		case R.id.status:
			int status = cursor.getInt(cursor.getColumnIndexOrThrow(MyDBHelper.KEY_SYNCED));
			/*CheckBox cb = (CheckBox) view;
			cb.setChecked(status==1?true:false);
			cb.setClickable(false);*/
			TextView tv = (TextView) view;
			tv.setText(status==1?"Synced":"Not synced");
			return true;
		default:
			return false;
		}
	}
}