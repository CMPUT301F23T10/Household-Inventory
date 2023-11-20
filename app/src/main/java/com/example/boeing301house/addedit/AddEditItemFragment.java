package com.example.boeing301house.addedit;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.boeing301house.DBConnection;
import com.example.boeing301house.Item;
import com.example.boeing301house.R;
import com.example.boeing301house.scraping.GoogleSearchThread;
import com.example.boeing301house.scraping.SearchUIRunnable;
import com.example.boeing301house.databinding.FragmentAddEditItemBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.chip.Chip;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.snackbar.Snackbar;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import org.apache.commons.io.FileUtils;


/**
 * Fragment for Adding and Editing an {@link Item}
 * Observer pattern used
 */
public class AddEditItemFragment extends Fragment {
    private static final String TAG = "ADD_EDIT_FRAG";
    /**
     * Current item being added/edited
     */
    private Item currentItem;
    /**
     * Tag for getting item from bundle
     */
    public static String ITEM_KEY = "item_key";

    /**
     * Tag to determine if adding item
     */
    public static String IS_ADD = "is_add";

    /**
     * Binding for fragment
     */
    private FragmentAddEditItemBinding binding; //used to access the things in add_edit_item_fragment_view.xml

    /**
     * New make string
     */
    private String newMake;

    /**
     * New model string
     */
    private String newModel;

    /**
     * New value
     */
    private double newValue;

    /**
     * New comment string
     */
    private String newComment;

    /**
     * New description string
     */
    private String newDescription;

    /**
     * New date
     * Default null for editing functionality
     */
    private Long newDate = null;

    private ArrayList<String> newTags;

    /**
     * New SN
     */
    private String newSN;

    /**
     * RecyclerView for images
     */
    private RecyclerView imgRecyclerView;

    /**
     * Adapter for RecyclerView
     */
    private AddEditImageAdapter imgAdapter;

    /**
     * ArrayList of uris
     */
    private ArrayList<Uri> uri;

    private boolean isAwaiting = false;

    private Uri newURI;

    private File imgPath;


    private static final int READ_PERMISSIONS = 101;
    private static final int CAMERA_PERMISSIONS = 102;
    private static final int WRITE_PERMISSIONS = 103; // FOR API < 32
    private static final int GALLERY_REQUEST = 110;
    private static final int CAMERA_REQUEST = 111;
//    private static final int SCAN_BARCODE_REQUEST = 112;
//    private static final int SCAN_SN_REQUEST = 113;


    FirebaseStorage storage = FirebaseStorage.getInstance("gs://boeing301house.appspot.com");
    StorageReference storageRef = storage.getReference();

    /**
     * listener for addedit interaction (sends results back to caller)
     */
    private OnAddEditFragmentInteractionListener listener;
    private boolean isAdd = true; // if adding item

    // TODO: finish javadoc
    /**
     * Listener object for Adding/Editing {@link Item}. Uses Observer pattern.
     */
    public interface OnAddEditFragmentInteractionListener {
        void onCancel();


        void onConfirmPressed(Item updatedItem);
    }

    // https://stackoverflow.com/questions/9931993/passing-an-object-from-an-activity-to-a-fragment
    // handle passing through an expense object to fragment from activity

    /**
     * This function creates an instance of the fragment and passes an {@link Item} to it.
     * Creates fragment via a no-argument constructor
     * @param item Parcelable {@link Item} object given to fragment
     * @param isAdd boolean (true if adding, false if editing)
     * @return fragment instance
     */
    public static AddEditItemFragment newInstance(Item item, boolean isAdd, String[] uriStrings) {
        AddEditItemFragment fragment = new AddEditItemFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ITEM_KEY, item);
        bundle.putBoolean(IS_ADD, isAdd);
        bundle.putStringArray("URIS", uriStrings);
        fragment.setArguments(bundle);

        return fragment;
    }

    // this is the code that will allow the transfer of the updated expense to the main listview
    /**
     * Called when fragment attached
     * @param context: app context
     */
    @Override
    public void onAttach(Context context){
        super.onAttach(context);

        if(context instanceof OnAddEditFragmentInteractionListener){
            listener = (OnAddEditFragmentInteractionListener) context;

        }
        else{
            throw new RuntimeException(context.toString() + "must implement onfraglistener");
        }
    }
    // TODO: finish javadoc
    /**
     *
     * @param savedInstanceState If the fragment is being re-created from
     * a previous saved state, this is the state.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentItem = (Item) getArguments().getParcelable("item_key"); // get item from bundle
            isAdd = (boolean) getArguments().getBoolean(IS_ADD);

        }
    }

    // TODO: finish javadoc
    /**
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return created view
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAddEditItemBinding.inflate(inflater, container, false); //this allows me to accsess the stuff!
        View view = binding.getRoot();

        imgPath = new File(requireContext().getFilesDir(), "images");
        if (!imgPath.exists()) {
            imgPath.mkdirs();
        }

        uri = new ArrayList<>();
        newTags = new ArrayList<>(currentItem.getTags());

        imgRecyclerView = binding.addEditImageRecycler;
        imgAdapter = new AddEditImageAdapter(uri);

        imgAdapter.setOnClickListener(new ImageSelectListener() {
            @Override
            public void onItemClicked(int position) {
                // updating firebase when we delete an image
                updateFirebaseImages(false, (Integer) position);
                uri.remove(position);
                imgAdapter.notifyDataSetChanged();
            }
        });

        // GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 4);
        // LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        // layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        // imgRecyclerView.setLayoutManager(layoutManager);
        imgRecyclerView.setAdapter(imgAdapter);

        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 104);
        }


//        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_MEDIA_IMAGES)) {
//
//        }


        binding.itemAddEditMaterialToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: add functionality and check over
                listener.onCancel();
                // deleteFrag();
            }
        });


        binding.itemAddEditMaterialToolBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            // TODO: allow backing from fragment to fragment
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                /*
                if (item.getItemId() == R.id.itemAddEditTag) {
                    Toast.makeText(getActivity(), String.format(Locale.CANADA,"WIP/INCOMPLETE"),
                            Toast.LENGTH_SHORT).show(); // for testing
                    Fragment tagsFragment = TagsFragment.newInstance(currentItem);
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.itemAddEditContent, tagsFragment, "UPDATE_TO_TAG")
                            .addToBackStack(null)
                            .commit();

                    return true;

                } else

                 */
                if (item.getItemId() == R.id.itemAddEditPhotoButton) {
                    // add camera functionality
                    View menuItemView = view.findViewById(item.getItemId());
                    PopupMenu popup = new PopupMenu(getActivity(), menuItemView);

                    popup.getMenuInflater().inflate(R.menu.image_popup_menu, popup.getMenu());
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            if (item.getItemId() == R.id.camera) {
                                return askCameraPerms(CAMERA_REQUEST);
                            }
                            else if (item.getItemId() == R.id.gallery) {
                                return askGalleryPerms();
                            }
                            return false;
                        }
                    });

                    popup.setOnDismissListener(new PopupMenu.OnDismissListener() {
                        @Override
                        public void onDismiss(PopupMenu menu) {
                            return;
                        }
                    });

                    popup.show();

                    return true;


                } else if (item.getItemId() == R.id.itemAddEditScanButton) {
                    // add scanning functionality
                    View menuItemView = view.findViewById(item.getItemId());
                    PopupMenu popup = new PopupMenu(getActivity(), menuItemView);

                    popup.getMenuInflater().inflate(R.menu.scan_popup_menu, popup.getMenu());

                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            if (item.getItemId() == R.id.scanBarcode) {
                                return askCameraPerms(ScannerActivity.SCAN_BARCODE_REQUEST);
                            }
                            else if (item.getItemId() == R.id.scanSN) {
                                return askCameraPerms(ScannerActivity.SCAN_SN_REQUEST);
                            }
                            return false;
                        }
                    });

                    popup.setOnDismissListener(new PopupMenu.OnDismissListener() {
                        @Override
                        public void onDismiss(PopupMenu menu) {
                            return;
                        }
                    });

                    popup.show();

                }
                return false;
            }
        });

        //this sets the current text of the edit expense fragment to the current expense name, cost, date and summary
//        View view = inflater.inflate(R.layout.add_edit_item_fragment, container, false);
//        EditText editCost = view.findViewById(R.id.editCost);
//        editCost.setText(currentItem.getCostString());
        binding.updateValue.setHint(String.format("Cost: $%s", currentItem.getValueString()));
        binding.updateMake.setHint(String.format("Make: %s", currentItem.getMake()));
        binding.updateModel.setHint(String.format("Model: %s", currentItem.getModel()));
        if (isAdd) {
            binding.updateDate.setHint("Date Acquired: mm/dd/yyyy");
        } else {
            binding.updateDate.setHint(String.format("Date Acquired: %s", currentItem.getDateString()));
        }

        binding.updateSN.setHint(String.format("SN: %s", currentItem.getSN()));
        binding.updateComment.setHint(String.format("Comment: %s", currentItem.getComment()));
        binding.updateDesc.setHint(String.format("Desc: %s", currentItem.getDescription()));

        fillChipGroup();

        binding.updateTags.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                return;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                return;
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    if (s.charAt(s.length() - 1) == ' ' || s.charAt(s.length() - 1) == '\n') {
                        if (s.length() > 1 && (!newTags.contains(s.toString().trim()))) {
                            newTags.add(s.toString().trim());
                            addChip(s.toString().trim());
                        }
                        s.clear();
                    }
                }

            }
        });

        // create instance of material date picker builder
        //  creating datepicker
        MaterialDatePicker.Builder<Long> materialDateBuilder = MaterialDatePicker.Builder.datePicker();

        // create constraint for date picker (only let user choose dates on and before current"
        CalendarConstraints dateConstraint = new CalendarConstraints.Builder().setValidator(DateValidatorPointBackward.now()).build();

        // define properties/title for material date picker
        materialDateBuilder.setTitleText("Date Acquired: ");
        materialDateBuilder.setCalendarConstraints(dateConstraint); // apply date constraint
        materialDateBuilder.setSelection(Calendar.getInstance().getTimeInMillis());
        // instantiate date picker
        final MaterialDatePicker<Long> materialDatePicker = materialDateBuilder.build();

        final TimeZone local = Calendar.getInstance().getTimeZone(); // local timezone

        binding.updateDate.getEditText().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                materialDatePicker.show(getActivity().getSupportFragmentManager(), "MATERIAL_DATE_PICKER");


            }
        });


        // material date picker behaviours
        materialDatePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener<Long>() {
            @Override
            public void onPositiveButtonClick(Long selection) {
                // since material design picker date is in UTC
                long offset = local.getOffset(selection);
                long localDate = selection - offset; // account for timezone difference
                String dateString = new SimpleDateFormat("MM/dd/yyyy", Locale.CANADA).format(localDate);
                binding.updateDate.getEditText().setText(dateString);
                newDate = localDate;
            }
        });

        materialDatePicker.addOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.cancel(); // cancel dialog
            }
        });


        binding.updateItemConfirm.setOnClickListener(new View.OnClickListener() { //when clicked confirm button
            @Override
            public void onClick(View view) {
                newMake = binding.updateMake.getEditText().getText().toString();
                newModel = binding.updateModel.getEditText().getText().toString();
                String newValueString = binding.updateValue.getEditText().getText().toString();
                newComment = binding.updateComment.getEditText().getText().toString();
                newSN = binding.updateSN.getEditText().getText().toString();
                newDescription = binding.updateDesc.getEditText().getText().toString();

                String inputString = binding.updateTags.getEditText().getText().toString();
                if (!inputString.isEmpty()) {
                    newTags.add(inputString.trim());
                }

                if (isAdd)
                {
                    if(!checkFields()) {
                        newValue = Double.parseDouble(newValueString);

                        // setting the current item with the new fields
                        currentItem.setComment(newComment);
                        currentItem.setMake(newMake);
                        currentItem.setModel(newModel);
                        currentItem.setDate(newDate);
                        currentItem.setValue(newValue);
                        currentItem.setSN(newSN);
                        Log.d("TAG TEST", "Size: " + newTags.size());
                        currentItem.setDescription(newDescription);
                        currentItem.setTags(newTags);

                        try {
                            deleteRecursive(imgPath);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        listener.onConfirmPressed(currentItem); // transfers the new data to main
                    }

                } else {
                    // during edit: replace empty fields with item values
                    if (StringUtils.isBlank(newMake)) {
                        newMake = currentItem.getMake();
                    }
                    if (StringUtils.isBlank(newComment)) {
                        newComment = currentItem.getComment();
                    }
                    if (StringUtils.isBlank(newModel)) {
                        newModel = currentItem.getModel();
                    }
                    if (newDate == null) {
                        newDate = currentItem.getDate();
                    }
                    if (StringUtils.isBlank(newValueString)) {
                        newValue = currentItem.getValue();
                    } else {
                        newValue = Double.parseDouble(newValueString);
                    }
                    if (StringUtils.isBlank(newSN)) {
                        newSN = currentItem.getSN();
                    }
                    if (StringUtils.isBlank(newDescription)) {
                        newDescription = currentItem.getDescription();
                    }
                    // setting the values of the current item
                    currentItem.setComment(newComment);
                    currentItem.setMake(newMake);
                    currentItem.setModel(newModel);
                    currentItem.setDate(newDate);
                    currentItem.setValue(newValue);
                    currentItem.setSN(newSN);
                    currentItem.setDescription(newDescription);
                    currentItem.setTags(newTags);
                    listener.onConfirmPressed(currentItem);

                }

            }
        });
        return view;
    }

    /**
     * Destroys the view
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Checks if any required fields were left blank, if any of them were left blank
     * it alerts the user.
     * @return boolean
     */
    private boolean checkFields() {
        boolean isError = false;
        // reset errors
        binding.updateModel.setError("");
        binding.updateMake.setError("");
        binding.updateValue.setError("");
        binding.updateDate.setError("");

        Long currentDate = Calendar.getInstance(Locale.CANADA).getTimeInMillis();

        if (Objects.requireNonNull(binding.updateModel.getEditText()).length() == 0) {
            binding.updateModel.setError("This field is required");
            isError = true;
        }
        if (Objects.requireNonNull(binding.updateMake.getEditText()).length() == 0) {
            binding.updateMake.setError("This field is required");
            isError = true;
        }
        if (Objects.requireNonNull(binding.updateValue.getEditText()).length() == 0) {
            binding.updateValue.setError("This field is required");
            isError = true;
        }
        if (Objects.requireNonNull(binding.updateDate.getEditText()).length() == 0) {
            binding.updateDate.setError("This field is required");
            isError = true;
        }
//        } else if (newDate > currentDate) {
//            binding.updateDate.setError("Invalid Date");
//            isError = true;
//        }

        return isError;
    }


    /**
     * ActivityResult for img from gallery
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode The integer result code returned by the child activity
     *                   through its setResult().
     * @param data An Intent, which can return result data to the caller
     *               (various data can be attached to Intent "extras").
     *
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
        Log.d("CAMERA_TEST", "rqC: " + requestCode + " | rC: " + resultCode);
        if (requestCode == GALLERY_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data.getClipData() != null) {
                int x = data.getClipData().getItemCount();

                for (int i = 0; i < x; i++) {
                    newURI = data.getClipData().getItemAt(i).getUri();
                    uri.add(data.getClipData().getItemAt(i).getUri());
                    String imgURI = data.getClipData().getItemAt(i).getUri().toString();
//                    Log.d("CAMERA_TEST", imgURI);
                    // adding from gallery
                    updateFirebaseImages(true, null);

                }
                imgAdapter.notifyDataSetChanged();
            } else if (data.getData() != null) {
                Uri imgURI = data.getData();
                newURI = imgURI;
//                Log.d("CAMERA_TEST", imgURI);
                uri.add(imgURI);
                // adding from gallery
                updateFirebaseImages(true, null);

            }
            imgAdapter.notifyDataSetChanged();
        } else if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            Log.d("CAMERA_TEST", newURI.toString());

            uri.add(newURI);
            imgAdapter.notifyDataSetChanged();

            // adding a image from camera
            updateFirebaseImages(true, null);

        } else if (requestCode == ScannerActivity.SCAN_BARCODE_REQUEST && resultCode == Activity.RESULT_OK) { // TODO: CONVERT TO SCANNER INTENT
            if (data.getExtras() != null) {
                Bitmap image = (Bitmap) data.getExtras().get("data");
                assert image != null;
                scanBarcode(image);
            }
        } else if (requestCode == ScannerActivity.SCAN_SN_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data.getExtras() != null) {
                String SN = data.getStringExtra(ScannerActivity.RETURN_SN);
                if (SN == null) {
                    return;
                }
                if (SN.isEmpty()) {
                    Snackbar.make(binding.itemAddEditContent, "FAILED TO READ SN", Snackbar.LENGTH_SHORT).show();
                } else {
                    binding.updateSN.getEditText().setText(SN);
                }
            }
        }

    }


    /**
     * Get permission to use gallery and open gallery
     * @return true if gallery opened, false otherwise
     */
    private boolean askGalleryPerms() {
        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
            ActivityCompat.requestPermissions(requireActivity(), new String[] {Manifest.permission.READ_MEDIA_IMAGES}, READ_PERMISSIONS);
            return false;
        } else if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            ActivityCompat.requestPermissions(requireActivity(), new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, READ_PERMISSIONS);
            return false;
        }
//        else {
//            openGallery();
//            return true;
//        }
        if ((ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.READ_MEDIA_IMAGES)
                == PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED)) {
            openGallery();
            return true;
        }
        return false;
    }

    /**
     * Open photo gallery, let user select multiple images to bring back
     */
    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // allow user to select + return multiple item
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Image(s)"), GALLERY_REQUEST);
        // open gallery
    }

    /**
     * Get permission to use camera and open camera
     * @return true if camera/scanner opened, false otherwise
     */
    private boolean askCameraPerms(int requestCode) {
        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            ActivityCompat.requestPermissions(requireActivity(), new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_PERMISSIONS);
        }

        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[] {Manifest.permission.CAMERA}, CAMERA_PERMISSIONS);
            return false;

        }
        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == ScannerActivity.SCAN_SN_REQUEST) {
                openSNCamera();
                return true;
            }
            else {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
                    openCamera(requestCode);
                    return true;
                } else if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    openCamera(requestCode);
                    return true;
                } else { return false; }
            }
        } else { return false; }
    }


    /**
     * Fill chip group w/ item tags (for initializing)
     */
    public void fillChipGroup() {
        for (int i = 0; i < newTags.size(); i++) {
            final String name = newTags.get(i);
            final Chip newChip = new Chip(requireContext());
            newChip.setText(name);
            newChip.setCloseIconResource(R.drawable.ic_close_button_24dp);
            newChip.setCloseIconEnabled(true);
            newChip.setContentDescription("chip"+name);
            newChip.setCloseIconContentDescription("close"+name); // for ui testing
            newChip.setOnCloseIconClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    newTags.remove(name);
                    binding.itemAddEditChipGroup.removeView(newChip);
                }
            });

            binding.itemAddEditChipGroup.addView(newChip);
        }
    }


    /**
     * Add chip to chip group
     * @param tag tag to add
     */
    private void addChip(String tag) {
        final String name = tag;
        final Chip newChip = new Chip(requireContext());
        newChip.setText(name);
        newChip.setCloseIconResource(R.drawable.ic_close_button_24dp);
        newChip.setCloseIconEnabled(true);
        newChip.setOnCloseIconClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newTags.remove(name);
                binding.itemAddEditChipGroup.removeView(newChip);
            }
        });

        binding.itemAddEditChipGroup.addView(newChip);

    }

    /**
     * Open camera (overloaded function for use w/ scanning)
     */
    public void openCamera(Integer requestCode) {
        Intent intent = new Intent();
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        if (requestCode == CAMERA_REQUEST) {
            // https://developer.android.com/reference/android/support/v4/content/FileProvider.html
            // store img at new path and remember URI
//            imgPath = new File(requireContext().getFilesDir(), "images");
//            if (!imgPath.exists()) {
//                imgPath.mkdirs();
//            }
            // creating the new file path
            File newFile = new File(imgPath, System.currentTimeMillis() + ".jpg");
            newURI = FileProvider.getUriForFile(requireContext(), "com.example.boeing301house", newFile);


            intent.putExtra(MediaStore.EXTRA_OUTPUT, newURI);
            Log.d("CAMERA_TEST", "PATH CREATED");
        }

        startActivityForResult(intent, requestCode);
    }

    /**
     * Updates firebase and stores the image when a image is added and deletes the image from firebase upon deleting too.
     * @param adding Boolean to check if were adding a image
     * @param position Integer of the position in the URI array list
     */
    private void updateFirebaseImages(boolean adding, Integer position) {
        // adding is true and position is null means were adding
        if (adding == true && position == null) {
            Uri fileUri = newURI;
            StorageReference ref = storageRef.child("images/" + fileUri.getLastPathSegment());
            UploadTask uploadTask = ref.putFile(fileUri);
            Log.d("PHOTO_UPLOADED", "updateFirebaseImages: WORKED");

            // Register observers to listen for when the download is done or if it fails
            uploadTask.addOnFailureListener(exception -> {
                return;
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // message for succesful upload
                    Log.d("PHOTO_ADDED", "onSuccess: YESSSS");
                }
            });
        }
        // else if were deleting an existing image
        else if (position != null) {
            // getting the position
            String result = uri.get(position).getPath();
            int cut = result.lastIndexOf('/'); // formating the string
            if (cut != -1) {
                result = result.substring(cut + 1);
                storageRef.child("images/" + result).
                // Delete the file
                delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("PHOTO_DELETED", "onSuccess: DELETED");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Uh-oh, an error occurred!
                    }
                });
            }
        }
    }

    /**
     * Deletes folder directory on the phone and children too
     * @param directory
     */
    void deleteRecursive(File directory) throws IOException {
        // delete the folder on device
        FileUtils.deleteDirectory(directory);
    }
    /**
     * Open custom camera for SN
     */
    public void openSNCamera() {
        Intent intent = new Intent(getActivity(), ScannerActivity.class);
        startActivityForResult(intent, ScannerActivity.SCAN_SN_REQUEST); // result -> String if SN found, null otherwise

    }

    /**
     * Scan barcode via img from camera
     * <a href="https://developers.google.com/ml-kit/vision/barcode-scanning/android">...</a>
     */
    public void scanBarcode(Bitmap image) {
        Snackbar snackbar = Snackbar.make(binding.itemAddEditContent, "PROCESSING BARCODE...", Snackbar.LENGTH_LONG);
        snackbar.setAction("DISMISS", v -> {
            snackbar.dismiss();
        });
        snackbar.show();

        Log.d("SEARCH", "BARCODE FUNC CALLED");
        final ArrayList<String> productInfo = new ArrayList<>();
        Log.d(TAG, "Image: " + image.toString());
        InputImage inputImage = InputImage.fromBitmap(image, 0);

        BarcodeScanner barcodeScanner = BarcodeScanning.getClient();
        barcodeScanner.process(inputImage).addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
            @Override
            public void onSuccess(List<Barcode> barcodes) {
                Log.d("SEARCH", "BARCODE FUNC CALLED");
                Log.d("SEARCH", "Successfully processed barcode. # " + barcodes.size());
                Log.d(TAG, "Successfully processed barcode. # " + barcodes.size());
                if (barcodes.size() == 0) {
                    snackbar.setDuration(Snackbar.LENGTH_SHORT).setText("NO BARCODE DETECTED").show();
                }
                for (Barcode barcode: barcodes) {
                    String barcodeData = barcode.getRawValue();

                    getBarcodeData(barcodeData);

                    productInfo.add(barcodeData);
                    Log.d("SEARCH", "BARCODE SCANNED IN ADD EDIT");
                    Log.d(TAG, barcode.getRawValue());
                }

                // binding.updateDesc.getEditText().setText(StringUtils.join(productInfo, ". "));
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "Failed to process barcode");
                snackbar.setDuration(Snackbar.LENGTH_SHORT).setText("FAILED TO PROCESS").show();
            }
        });
    }

    /**
     * Run web search for product info
     * @param barcode item barcode
     */
    public void getBarcodeData(String barcode) {
        Snackbar snackbar = Snackbar.make(binding.itemAddEditContent, "PROCESSING BARCODE...", Snackbar.LENGTH_LONG);
        snackbar.setAction("DISMISS", v -> {
            snackbar.dismiss();
        });

        GoogleSearchThread thread = new GoogleSearchThread(barcode, result -> {
            if (result != null) {
                SearchUIRunnable searchRunnable = new SearchUIRunnable(result, title -> {
                    if (binding != null)
                        if (title != null) {
                            snackbar.setDuration(Snackbar.LENGTH_SHORT).setText("PROCESSED").show();
                            binding.updateDesc.getEditText().setText(title);
                        } else {
                            snackbar.setDuration(Snackbar.LENGTH_SHORT).setText("NO INFO FOUND").show();
                            binding.updateDesc.getEditText().setText(barcode);
                        }
                });

                if (binding != null) {
                    getActivity().runOnUiThread(searchRunnable);
                }
            }
        });


        thread.start();

    }



}