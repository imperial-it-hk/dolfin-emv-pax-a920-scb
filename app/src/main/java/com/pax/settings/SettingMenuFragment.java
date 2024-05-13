/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-11-30
 * Module Author: Steven.W
 * Description:
 *
 * ============================================================================
 */
package com.pax.settings;

import android.app.Fragment;
import android.os.Bundle;
import th.co.bkkps.utils.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.pax.edc.R;
import com.pax.pay.app.FinancialApplication;
import com.pax.settings.host.AcquirerFragment;
import com.pax.settings.host.EDCFragment;
import com.pax.settings.host.IssuerParamFragment;

import java.util.LinkedList;
import java.util.List;

public class SettingMenuFragment extends Fragment {
    public static final String TAG = "SettingsMenuFragment";
    private int mCurCheckPosition = 0;
    private ListView mListView;
    private SettingMainMenuListAdapter mAdapter;
    private List<SettingMainMenuListAdapter.Item> mData;

    private boolean isFirst = FinancialApplication.getController().isFirstRun();

    private static final List<SettingMainMenuListAdapter.Item> listItems = new LinkedList<>();
    private static final List<SettingMainMenuListAdapter.Item> initListItems = new LinkedList<>();

    static {
        listItems.add(new SettingMainMenuListAdapter.Item(R.string.settings_menu_communication_parameter, CommParamFragment.class));
        listItems.add(new SettingMainMenuListAdapter.Item(R.string.settings_menu_edc_parameter, EDCFragment.class));
        listItems.add(new SettingMainMenuListAdapter.Item(R.string.settings_menu_issuer_parameter, IssuerParamFragment.class));
        listItems.add(new SettingMainMenuListAdapter.Item(R.string.settings_menu_acquirer_parameter, AcquirerFragment.class));
        listItems.add(new SettingMainMenuListAdapter.Item(R.string.settings_menu_keyManage, KeyManageFragment.class));
        listItems.add(new SettingMainMenuListAdapter.Item(R.string.settings_menu_pwd_manage, PwdManageDetailFragment.class));
        listItems.add(new SettingMainMenuListAdapter.Item(R.string.settings_menu_otherManage, OtherManageFragment.class));
        listItems.add(new SettingMainMenuListAdapter.Item(R.string.settings_menu_linkposManage, LinkPOSManageFragment.class));

        initListItems.add(new SettingMainMenuListAdapter.Item(R.string.settings_menu_communication_parameter, CommParamFragment.class));
        initListItems.add(new SettingMainMenuListAdapter.Item(R.string.settings_menu_keyManage, KeyManageFragment.class));
        initListItems.add(new SettingMainMenuListAdapter.Item(R.string.settings_menu_pwd_manage, PwdManageDetailFragment.class));
        initListItems.add(new SettingMainMenuListAdapter.Item(R.string.settings_menu_otherManage, OtherManageFragment.class));
    }

    private final OnItemClickListener itemClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
            showDetails(index, 1);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mData = isFirst ? initListItems : listItems;
        View rootView = inflater.inflate(R.layout.setting_options_list, container, false);
        mListView = (ListView) rootView.findViewById(R.id.item_list);
        mAdapter = new SettingMainMenuListAdapter(getActivity(), mData);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(itemClickListener);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            // Restore last state for checked position.
            mCurCheckPosition = savedInstanceState.getInt("curChoice", 0);
        }
        // In dual-pane mode, the list view highlights the selected item_gridview.
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        showDetails(mCurCheckPosition, 0);
    }

    private void showDetails(int index, int flag) {
        if ((flag == 1) && (mCurCheckPosition == index))
            return;

        mAdapter.setSelectItem(index);
        mAdapter.notifyDataSetInvalidated();

        try {
            Fragment fragment = (Fragment) mData.get(index).cls.newInstance();
            if (fragment != null) {
                mCurCheckPosition = index;
                getFragmentManager().beginTransaction().replace(R.id.settings_main_detail, fragment).commit();
            }
        } catch (java.lang.InstantiationException | IllegalAccessException e) {
            Log.e(TAG, "", e);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("curChoice", mCurCheckPosition);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.add("Menu 1a").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add("Menu 1b").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Toast.makeText(getActivity(), " && menu text is " + item.getTitle(), Toast.LENGTH_SHORT).show();
        return super.onOptionsItemSelected(item);
    }

}
