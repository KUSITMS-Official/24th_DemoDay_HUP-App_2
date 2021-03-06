package com.example.auctionapp.domain.item.view;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.auctionapp.R;
import com.example.auctionapp.domain.item.constant.ItemConstants;
import com.example.auctionapp.domain.item.dto.GetAllItemsByStatusRequest;
import com.example.auctionapp.domain.item.dto.ItemDetailsResponse;
import com.example.auctionapp.domain.pricesuggestion.dto.BidderResponse;
import com.example.auctionapp.domain.pricesuggestion.dto.MaximumPriceResponse;
import com.example.auctionapp.domain.user.constant.Constants;
import com.example.auctionapp.global.dto.PaginationDto;
import com.example.auctionapp.global.retrofit.MainRetrofitCallback;
import com.example.auctionapp.global.retrofit.MainRetrofitTool;
import com.example.auctionapp.global.retrofit.RetrofitTool;

import org.json.JSONException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import retrofit2.Response;

import static android.content.ContentValues.TAG;

public class SellHistoryEnd extends Fragment {
    ViewGroup viewGroup;

    SellHistoryEndAdapter adapter;
    SellHistoryEndData data;
    List<SellHistoryEndData> sellHistoryEndDataList;
    int bidderCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        viewGroup = (ViewGroup) inflater.inflate(R.layout.activity_sell_history_end, container, false);

        init();
        getData();

        return viewGroup;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
    private void init(){
        RecyclerView recyclerView = viewGroup.findViewById(R.id.sellHistoryEndRecyclerView);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(linearLayoutManager);

        adapter = new SellHistoryEndAdapter();
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(new SellHistoryEndAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
//                Intent intent = new Intent(getContext(), ItemDetail.class);
//                startActivity(intent);
            }
        });

        //?????????
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), new LinearLayoutManager(getContext()).getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);
    }
    private void getData(){
        //?????? ???????????????
//        SellHistoryEndData data = new SellHistoryEndData(R.drawable.rectangle, "????????? ?????????", 197000, "????????????");
//        adapter.addItem(data);
//        data = new SellHistoryEndData(R.drawable.rectangle, "CGV ????????????", 6500, "????????????");
//        adapter.addItem(data);
        bidderCount = 0;
        sellHistoryEndDataList = new ArrayList<>();
        RetrofitTool.getAPIWithAuthorizationToken(Constants.token).getAllItemsByUserIdAndStatus(GetAllItemsByStatusRequest.of(Constants.userId, ItemConstants.EItemSoldStatus.eSoldOut))
                .enqueue(MainRetrofitTool.getCallback(new getAllItemsByUserIdAndStatusCallback()));
    }

    private class getAllItemsByUserIdAndStatusCallback implements MainRetrofitCallback<PaginationDto<List<ItemDetailsResponse>>> {

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onSuccessResponse(Response<PaginationDto<List<ItemDetailsResponse>>> response) {
            for(int i=0; i<response.body().getData().size(); i++){
                LocalDateTime startDateTime = LocalDateTime.now();
                LocalDateTime endDateTime = response.body().getData().get(i).getAuctionClosingDate();
                String days = String.valueOf(ChronoUnit.DAYS.between(startDateTime, endDateTime));
                String hours = String.valueOf(ChronoUnit.HOURS.between(startDateTime, endDateTime));
                String minutes = String.valueOf(ChronoUnit.MINUTES.between(startDateTime, endDateTime));
                Long itemId = response.body().getData().get(i).getId();
                String fileNameMajor = response.body().getData().get(i).getFileNames().get(0);
                String itemName = response.body().getData().get(i).getItemName();
                int suggestionPrice = response.body().getData().get(i).getSoldPrice();

                if (response.body().getData().get(i).getFileNames().size() != 0) {
                    data = new SellHistoryEndData(itemId,
                            fileNameMajor,
                            itemName,
                            suggestionPrice,
                            null);
                } else {
                    data = new SellHistoryEndData(itemId,
                            null,
                            itemName,
                            suggestionPrice,
                            null);
                }
                sellHistoryEndDataList.add(data);
                RetrofitTool.getAPIWithAuthorizationToken(Constants.token).getBidder(itemId)
                        .enqueue(MainRetrofitTool.getCallback(new getBidderCallback()));
            }
//                setAnimation();
            Log.d(TAG, "retrofit success, idToken: " + response.body().toString());
        }

        @Override
        public void onFailResponse(Response<PaginationDto<List<ItemDetailsResponse>>> response) throws IOException, JSONException {
            System.out.println("errorBody"+response.errorBody().string());
            Log.d(TAG, "onFailResponse");
        }
        @Override
        public void onConnectionFail(Throwable t) {
            Log.e("????????????", t.getMessage());
        }
    }

    private class getBidderCallback implements MainRetrofitCallback<BidderResponse> {

        @Override
        public void onSuccessResponse(Response<BidderResponse> response) throws IOException {

            sellHistoryEndDataList.get(bidderCount).setBidderName(response.body().getBidderName());
            adapter.addItem(sellHistoryEndDataList.get(bidderCount));
            adapter.notifyDataSetChanged();
            Log.d(TAG, "retrofit success, idToken: " + response.body().toString());
            bidderCount++;
        }
        @Override
        public void onFailResponse(Response<BidderResponse> response) throws IOException, JSONException {
            System.out.println("errorBody"+response.errorBody().string());
            Log.d(TAG, "onFailResponse");
        }
        @Override
        public void onConnectionFail(Throwable t) {
            Log.e("????????????", t.getMessage());
        }
    }
}
@Getter
@Setter
class SellHistoryEndData {
    Long itemId;
    String imageURL;       //????????? ?????? (int -> string url)
    String itemName;
    int itemPrice;
    String bidderName;

    public SellHistoryEndData(Long itemId, String imageURL, String itemName, int itemPrice, String bidderName){
        this.itemId = itemId;
        this.imageURL = imageURL;
        this.itemName = itemName;
        this.itemPrice = itemPrice;
        this.bidderName = bidderName;
    }

}

class SellHistoryEndAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    // adapter??? ????????? list ?????????.
    private ArrayList<SellHistoryEndData> listData = new ArrayList<>();
    Context context;

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        context = recyclerView.getContext();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_sell_history_end_recyclerview, parent, false);
        return new SellHistoryEndViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((SellHistoryEndViewHolder)holder).onBind(listData.get(position));
    }

    @Override
    public int getItemCount() {
        return listData.size();
    }

    void addItem(SellHistoryEndData data) {
        // ???????????? item??? ???????????? ???????????????.
        listData.add(data);
    }

    public interface OnItemClickListener {
        void onItemClick(View v, int position) ;
    }
    // ????????? ?????? ????????? ???????????? ??????
    private OnItemClickListener mListener = null ;

    // OnItemClickListener ????????? ?????? ????????? ???????????? ???????????? ?????????
    public void setOnItemClickListener(OnItemClickListener listener) {

        this.mListener = listener ;
    }

    public class SellHistoryEndViewHolder extends  RecyclerView.ViewHolder {

        ImageView sell_history_end_img;
        TextView sell_history_end_edt_name;
        TextView sell_history_end_edt_myPrice;
        TextView sell_history_end_edt_seller;

        public SellHistoryEndViewHolder(@NonNull View itemView) {
            super(itemView);

            sell_history_end_img = itemView.findViewById(R.id.sell_history_end_img);
            sell_history_end_edt_name = itemView.findViewById(R.id.sell_history_end_edt_name);
            sell_history_end_edt_myPrice = itemView.findViewById(R.id.sell_history_end_edt_myPrice);
            sell_history_end_edt_seller = itemView.findViewById(R.id.sell_history_end_edt_seller);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition() ;
                    if (pos != RecyclerView.NO_POSITION) {
                        // ????????? ????????? ????????? ??????.
                        if (mListener != null) {
                            mListener.onItemClick(v, pos) ;
                        }
                    }
                }
            });

        }

        public void onBind(SellHistoryEndData data){
            Glide.with(context).load(Constants.imageBaseUrl+data.getImageURL()).override(sell_history_end_img.getWidth()
                    ,sell_history_end_img.getHeight()).into(sell_history_end_img);
            sell_history_end_img.setClipToOutline(true);  //item ?????????
            sell_history_end_edt_name.setText(data.getItemName());
            sell_history_end_edt_myPrice.setText(data.getItemPrice()+"");
            sell_history_end_edt_seller.setText(data.getBidderName());
        }
    }
}
