package com.example.customer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.databinding.FragmentCustomerPaymentsBinding;
import com.example.databinding.ItemPaymentCardBinding;
import com.example.utils.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CustomerPaymentsFragment extends Fragment {

    private FragmentCustomerPaymentsBinding binding;
    private CustomerViewModel viewModel;
    private PaymentListAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCustomerPaymentsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(CustomerViewModel.class);

        adapter = new PaymentListAdapter();
        binding.rvPayments.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvPayments.setAdapter(adapter);
        binding.swipeRefresh.setOnRefreshListener(this::loadPayments);

        loadPayments();
    }

    private void loadPayments() {
        viewModel.getPayments().observe(getViewLifecycleOwner(), res -> {
            if (res == null || binding == null) return;
            if (res.status == Resource.Status.LOADING) {
                binding.swipeRefresh.setRefreshing(true);
            } else if (res.status == Resource.Status.SUCCESS) {
                binding.swipeRefresh.setRefreshing(false);
                List<Map<String, Object>> data = res.data == null ? List.of() : res.data;
                adapter.submitList(data);
                binding.tvEmpty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
            } else if (res.status == Resource.Status.ERROR) {
                binding.swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class PaymentListAdapter extends RecyclerView.Adapter<PaymentListAdapter.VH> {
        private final List<Map<String, Object>> items = new ArrayList<>();

        void submitList(List<Map<String, Object>> list) {
            items.clear();
            items.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(ItemPaymentCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Map<String, Object> item = items.get(position);
            String jobCode = String.valueOf(item.get("jobCode"));
            String serviceName = String.valueOf(item.get("serviceName"));
            String method = String.valueOf(item.get("method"));
            String status = String.valueOf(item.get("status"));
            String ref = String.valueOf(item.get("transactionRef"));
            double amount = 0;
            try {
                amount = Double.parseDouble(String.valueOf(item.get("amount")));
            } catch (Exception ignored) {}

            holder.binding.tvTitle.setText(jobCode + " • " + serviceName);
            holder.binding.tvAmount.setText(String.format(Locale.getDefault(), "₹%.0f", amount));
            holder.binding.tvSubtitle.setText(method + " • " + status + (ref.isEmpty() ? "" : " • Ref: " + ref));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final ItemPaymentCardBinding binding;
            VH(ItemPaymentCardBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
