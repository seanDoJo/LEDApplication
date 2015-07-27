package Fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.cisco.prototype.ledsignaldetection.BluetoothInterface;
import com.cisco.prototype.ledsignaldetection.R;

import java.util.ArrayList;

public class SelectionFragment extends Fragment  {

    private BluetoothInterface mListener;
    private AbsListView mListView;
    private ArrayAdapter mAdapter;
    private ArrayList<String> myArray;
    private AdapterView.OnItemClickListener mMessageHandler = new AdapterView.OnItemClickListener(){
        public void onItemClick(AdapterView parent, View v, int position, long id){
            mListener.onSelectionFragment(position);
        }
    };
    public SelectionFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myArray = new ArrayList<String>();
        mAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, myArray);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_item, container, false);

        mListView = (AbsListView) view.findViewById(R.id.listView);
        ((AdapterView<ListAdapter>) mListView).setAdapter(mAdapter);

        mListView.setOnItemClickListener(mMessageHandler);
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try{
            mListener = (BluetoothInterface)activity;
        } catch(ClassCastException e){}
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void addDevice(String device){
        mAdapter.add(device);
    }

}
