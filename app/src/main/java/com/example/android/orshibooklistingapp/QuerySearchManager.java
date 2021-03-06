package com.example.android.orshibooklistingapp;

import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
//import android.support.v7.app.AlertController;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static android.R.attr.data;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;
import static android.webkit.ConsoleMessage.MessageLevel.LOG;
import static com.example.android.orshibooklistingapp.MainActivity.hideSoftKeyboard;


/**
 * Created by orsi on 07/06/2017.
 */

public class QuerySearchManager extends AppCompatActivity implements LoaderCallbacks<List<Book>> {

    public static final String LOG_TAG = QueryUtils.class.getName();

    public static final String GOOGLE_BOOKS_REQUEST_URL = "https://www.googleapis.com/books/v1/volumes?q=";
    public static final String END_OF_REQUEST = " ";
    private static final int BOOK_LOADER_ID = 1;
    ProgressBar progressBar;
    LoaderManager loaderManager;
    private TextView mEmptyStateTextView;

//    private BookAdapter mAdapter;//TODO torolni
    private static BookRecycleAdapter mRAdapter;
    private static RecyclerView bookRListView;
    private RecyclerView.LayoutManager layoutManager;

    private String finalRequestUrl;
    static View.OnClickListener myOnClickListener;


    public void fetchResults(String query) {
        Intent i = new Intent(getApplicationContext(), QuerySearchManager.class);
        i.putExtra("Title", query);
        startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bookRListView = (RecyclerView) findViewById(R.id.list);
        bookRListView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        bookRListView.setLayoutManager(layoutManager);
        bookRListView.setItemAnimator(new DefaultItemAnimator());

//        myOnClickListener = new MyOnClickListener(this);
//        bookRListView.setOnClickListener(myOnClickListener);


        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        progressBar = (ProgressBar) findViewById(R.id.loading_indicator);
        progressBar.setVisibility(View.VISIBLE);

        ImageView booksImage = (ImageView) findViewById(R.id.books);
        booksImage.setVisibility(View.GONE);

        String QueryStr = getIntent().getStringExtra("QueryStr");
        try {
            finalRequestUrl = GOOGLE_BOOKS_REQUEST_URL + URLEncoder.encode(QueryStr.toLowerCase(), "UTF-8")
                    + END_OF_REQUEST;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Log.e(LOG_TAG, finalRequestUrl);


        final SearchView searchView = (SearchView) findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                hideSoftKeyboard(QuerySearchManager.this);
                fetchResults(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        // Find a reference to the {@link ListView} in the layout
//        ListView bookListView = (ListView) findViewById(R.id.list); //TODO torolni


        //Set the empty state View
        mEmptyStateTextView = (TextView) findViewById(R.id.empty_view);

//        bookListView.setEmptyView(mEmptyStateTextView); //todo torolni
//        bookRListView.setEmptyView(mEmptyStateTextView); //TODO implementalni


        /* Adapter for the list of books */
//        mAdapter = new BookAdapter(this, new ArrayList<Book>()); //TODO torolni
        mRAdapter = new BookRecycleAdapter(new ArrayList<Book>(), new CustomItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                Log.d("TAG", "clicked position:" + position);
                String title = mRAdapter.getItem(position).getUrl();
            }
        });

        // Set the adapter on the ListView
        // so the list can be populated in the user interface
        bookRListView.setAdapter(mRAdapter);


        // Get a reference to the ConnectivityManager to check state of network connectivity
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);

        // Get details on the currently active default data network
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        // If there is a network connection, fetch data
        if (networkInfo != null && networkInfo.isConnected()) {
            // Get a reference to the LoaderManager, in order to interact with loaders.
            loaderManager = getLoaderManager();
            // Initialize the loader. Pass in the int ID constant defined above and pass in null for
            // the bundle. Pass in this activity for the LoaderCallbacks parameter (which is valid
            // because this activity implements the LoaderCallbacks interface).
            loaderManager.initLoader(BOOK_LOADER_ID, null, this);
        } else {
            // Otherwise, display error
            // First, hide loading indicator so error message will be visible
            View loadingIndicator = findViewById(R.id.loading_indicator);
            loadingIndicator.setVisibility(View.GONE);

            // Update empty state with no connection error message
            mEmptyStateTextView.setText(R.string.no_internet_connection);

        }
    }

    private static class MyOnClickListener implements View.OnClickListener {

        private final Context context;

        private MyOnClickListener(Context context) {
            this.context = context;
        }

        @Override
        public void onClick(View v) {
            Log.v("ORSI", "HELLO");
            openItem(v);
        }

        private void openItem(View v) {
            int selectedItemPosition = bookRListView.getChildPosition(v);

            // Find the current book that was clicked on
            Book currentBook = mRAdapter.getItem(selectedItemPosition);

            // Convert the String URL into a URI object (to pass into the Intent constructor)
            Uri bookUri = Uri.parse(currentBook.getUrl());

            // Create a new intent to view the earthquake URI
            Intent webIntent = new Intent(Intent.ACTION_VIEW, bookUri);

            // Send the intent to launch a new activity
            context.startActivity(webIntent);
        }
    }

    @Override
    public Loader<List<Book>> onCreateLoader(int i, Bundle bundle) {
        // Create a new loader for the given URL
        return new BookLoader(this, finalRequestUrl);
    }

    public void onLoadFinished(Loader<List<Book>> loader, List<Book> books) {
        // Hide loading indicator because the data has been loaded
        progressBar.setVisibility(View.GONE);

        // Set empty state text to display "No books found."
        mEmptyStateTextView.setText(R.string.no_books);

        // Clear the adapter of previous books data
//        mAdapter.clear(); //TODO torolni
        mRAdapter.clear();

        // If there is a valid list of {@link Book}s, then add them to the adapter's
        // data set. This will trigger the ListView to update.
        if (books != null && !books.isEmpty()) {
            mEmptyStateTextView.setVisibility(View.GONE);
//            mAdapter.addAll(books); //TODO torolni
            mRAdapter.addBooks(books);
        }
        else {
            mEmptyStateTextView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<Book>> loader) {
        // Loader reset, so we can clear out our existing data.
//        mAdapter.clear(); //todo torolni
        mRAdapter.clear();
    }
}
