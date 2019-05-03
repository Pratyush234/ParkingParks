package com.example.praty.parkingparks.adapter;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.praty.parkingparks.R;
import com.example.praty.parkingparks.helper.ItemClickListener;
import com.example.praty.parkingparks.model.ParkingSpaces;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import java.util.ArrayList;
import java.util.List;

public class ParkingAdapter extends RecyclerView.Adapter<ParkingAdapter.ViewHolder> {
    private static final String TAG = "ParkingAdapter";
    private Context mContext;
    private List<ParkingSpaces> mParkingSpaces;
    private DatabaseReference mRef;

    private ChildEventListener mListener=new ChildEventListener() {
        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            Log.d(TAG, "onChildAdded: children count:"+dataSnapshot.getChildrenCount());
            ParkingSpaces ps=dataSnapshot.getValue(ParkingSpaces.class);
            mParkingSpaces.add(ps);
            notifyDataSetChanged();
        }

        @Override
        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

        }

        @Override
        public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    };

    public ParkingAdapter(Context mContext,DatabaseReference mRef) {
        mParkingSpaces=new ArrayList<>();
        this.mContext=mContext;
        this.mRef=mRef.child("ParkingPlaces");
        this.mRef.addChildEventListener(mListener);


    }

    @NonNull
    @Override
    public ParkingAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v= LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_cardview,parent,false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ParkingAdapter.ViewHolder holder, final int position) {
        holder.address.setText(mParkingSpaces.get(position).getAddress());
        holder.description.setText(mParkingSpaces.get(position).getDescription());
        String availSlots="No. of slots available: "+mParkingSpaces.get(position).getSlots();
        holder.slots.setText(availSlots);

        Glide.with(mContext)
                .load(mParkingSpaces.get(position).getImageUri())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.imageView);

        //cardView click listener

        holder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //open Google maps app on the phone to drive to the clicked item's location

                Uri mapUri= Uri.parse("google.navigation:q="+mParkingSpaces.get(position).getLatitude()
                .toString()+","+mParkingSpaces.get(position).getLongitude().toString()+"&mode=d");

                Intent mapIntent=new Intent(Intent.ACTION_VIEW, mapUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                mContext.startActivity(mapIntent);
            }
        });

    }

    @Override
    public int getItemCount() {
        Log.d(TAG, "getItemCount: "+mParkingSpaces.size());
        return mParkingSpaces.size();
    }

    public void cleanup(){
        mRef.removeEventListener(mListener);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView address,description,slots;
        ImageView imageView;
        RelativeLayout parentLayout;
        public ViewHolder(View itemView) {
            super(itemView);

            address=(TextView) itemView.findViewById(R.id.address);
            description=(TextView) itemView.findViewById(R.id.description);
            slots=(TextView)itemView.findViewById(R.id.slots);
            imageView=(ImageView) itemView.findViewById(R.id.cardImage);
            parentLayout=(RelativeLayout) itemView.findViewById(R.id.parent_layout);
        }
    }
}
