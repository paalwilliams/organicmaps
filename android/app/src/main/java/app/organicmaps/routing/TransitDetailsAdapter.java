package app.organicmaps.routing;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import app.organicmaps.R;
import app.organicmaps.sdk.routing.TransitStepInfo;
import app.organicmaps.sdk.routing.TransitStepType;
import app.organicmaps.util.Utils;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders the expanded transit route breakdown: one row per leg, showing boarding stop, line badge,
 * stop count + time, and alighting stop. Walk legs are rendered as a single row.
 */
public class TransitDetailsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
  private static final int TYPE_WALK = 0;
  private static final int TYPE_RIDE = 1;

  @NonNull
  private final List<TransitStepInfo> mItems = new ArrayList<>();

  public void setItems(@NonNull List<TransitStepInfo> items)
  {
    mItems.clear();
    // The detail view only shows walks and transit rides. Intermediate-point markers and ruler
    // segments are not meaningful at this level of detail.
    for (TransitStepInfo info : items)
    {
      TransitStepType type = info.getType();
      if (type == TransitStepType.INTERMEDIATE_POINT || type == TransitStepType.RULER)
        continue;
      mItems.add(info);
    }
    notifyDataSetChanged();
  }

  @Override
  public int getItemViewType(int position)
  {
    return mItems.get(position).getType() == TransitStepType.PEDESTRIAN ? TYPE_WALK : TYPE_RIDE;
  }

  @NonNull
  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
  {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    if (viewType == TYPE_WALK)
      return new WalkViewHolder(inflater.inflate(R.layout.item_transit_details_walk, parent, false));
    return new RideViewHolder(inflater.inflate(R.layout.item_transit_details_ride, parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
  {
    TransitStepInfo info = mItems.get(position);
    if (holder instanceof WalkViewHolder)
      ((WalkViewHolder) holder).bind(info);
    else if (holder instanceof RideViewHolder)
    {
      // Suppress the "Board at <stop>" line when the previous row was already a ride: that
      // ride's "Transfer at <stop>" row showed the same stop name, so repeating it is noise.
      boolean prevIsRide = position > 0 && mItems.get(position - 1).getType() != TransitStepType.PEDESTRIAN;
      ((RideViewHolder) holder).bind(info, !prevIsRide, isLastRide(position));
    }
  }

  private boolean isLastRide(int position)
  {
    for (int i = position + 1; i < mItems.size(); i++)
    {
      if (mItems.get(i).getType() != TransitStepType.PEDESTRIAN)
        return false;
    }
    return true;
  }

  @Override
  public int getItemCount()
  {
    return mItems.size();
  }

  static class WalkViewHolder extends RecyclerView.ViewHolder
  {
    @NonNull
    private final TextView mText;

    WalkViewHolder(@NonNull View itemView)
    {
      super(itemView);
      mText = itemView.findViewById(R.id.walk_text);
    }

    void bind(@NonNull TransitStepInfo info)
    {
      Context ctx = itemView.getContext();
      CharSequence time = Utils.formatRoutingTime(ctx, info.getTimeInSec(), R.dimen.text_size_body_3);
      StringBuilder text = new StringBuilder(ctx.getString(R.string.transit_walk_label));
      text.append(" · ").append(time);
      if (info.getDistance() != null && !info.getDistance().isEmpty())
        text.append(" · ").append(info.getDistance()).append(' ').append(info.getDistanceUnits());
      mText.setText(text);
    }
  }

  static class RideViewHolder extends RecyclerView.ViewHolder
  {
    @NonNull
    private final TextView mBoardAt;
    @NonNull
    private final TextView mAlightAt;
    @NonNull
    private final TextView mSubtitle;
    @NonNull
    private final TextView mIntermediates;
    @NonNull
    private final TransitStepView mBadge;

    RideViewHolder(@NonNull View itemView)
    {
      super(itemView);
      mBoardAt = itemView.findViewById(R.id.board_at);
      mAlightAt = itemView.findViewById(R.id.alight_at);
      mSubtitle = itemView.findViewById(R.id.ride_subtitle);
      mIntermediates = itemView.findViewById(R.id.intermediate_stops);
      mBadge = itemView.findViewById(R.id.line_badge);
    }

    void bind(@NonNull TransitStepInfo info, boolean showBoardAt, boolean isLastRide)
    {
      Context ctx = itemView.getContext();
      mBadge.setTransitStepInfo(info);

      String startStop = info.getStartStopName();
      String endStop = info.getEndStopName();
      if (showBoardAt)
      {
        mBoardAt.setVisibility(View.VISIBLE);
        mBoardAt.setText(ctx.getString(R.string.transit_board_at, startStop == null ? "" : startStop));
      }
      else
      {
        mBoardAt.setVisibility(View.GONE);
      }
      int endLabelRes = isLastRide ? R.string.transit_exit_at : R.string.transit_transfer_at;
      mAlightAt.setText(ctx.getString(endLabelRes, endStop == null ? "" : endStop));

      CharSequence time = Utils.formatRoutingTime(ctx, info.getTimeInSec(), R.dimen.text_size_body_3);
      int stopCount = info.getStopCount();
      StringBuilder subtitle = new StringBuilder();
      if (stopCount > 0)
      {
        subtitle.append(ctx.getResources().getQuantityString(R.plurals.transit_stops_count, stopCount, stopCount));
        subtitle.append(" · ");
      }
      subtitle.append(time);
      mSubtitle.setText(subtitle);

      String[] intermediates = info.getIntermediateStopNames();
      if (intermediates == null || intermediates.length == 0)
      {
        mIntermediates.setVisibility(View.GONE);
      }
      else
      {
        mIntermediates.setVisibility(View.VISIBLE);
        mIntermediates.setText(String.join(" · ", intermediates));
      }
    }
  }
}
