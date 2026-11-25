package com.flexmeet.fragments;

import android.content.ContentValues;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.flexmeet.databinding.FragmentNotesBinding;
import com.flexmeet.firebase.FirebaseManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Date;

public class NotesFragment extends Fragment {

    private FragmentNotesBinding binding;
    private String meetingId;
    private DatabaseReference notesRef;
    private boolean isUpdatingLocally = false;

    public static NotesFragment newInstance(String meetingId) {
        NotesFragment fragment = new NotesFragment();
        Bundle args = new Bundle();
        args.putString("meeting_id", meetingId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            meetingId = getArguments().getString("meeting_id");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNotesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        notesRef = FirebaseManager.getInstance().getFirebaseDatabase()
                .getReference("meetings").child(meetingId).child("shared_notes");

        setupRealtimeSync();
        setupTextChangeListener();

        binding.closeNotes.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction().remove(this).commit();
        });

        binding.exportPdfBtn.setOnClickListener(v -> {
            exportNotesToPdf();
        });
    }

    private void setupRealtimeSync() {
        notesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isUpdatingLocally || binding == null) return;

                String content = snapshot.child("content").getValue(String.class);
                String lastEditedBy = snapshot.child("lastEditedBy").getValue(String.class);

                if (content != null && !content.equals(binding.notesEditText.getText().toString())) {
                    binding.notesEditText.setText(content);
                }
                if (lastEditedBy != null) {
                    binding.lastEditedStatus.setText("Last edited by: " + lastEditedBy);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null)
                    Toast.makeText(getContext(), "Sync failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupTextChangeListener() {
        binding.notesEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isUpdatingLocally = true;
                updateNotesInFirebase(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                isUpdatingLocally = false;
            }
        });
    }

    private void updateNotesInFirebase(String content) {
        String userName = FirebaseManager.getInstance().getCurrentUserEmail();
        notesRef.child("content").setValue(content);
        notesRef.child("lastEditedBy").setValue(userName);
        notesRef.child("timestamp").setValue(System.currentTimeMillis());
    }

    private void exportNotesToPdf() {
        String content = binding.notesEditText.getText().toString();
        if (content.isEmpty()) {
            Toast.makeText(getContext(), "Notes are empty", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        TextPaint textPaint = new TextPaint();
        textPaint.setColor(Color.BLACK);

        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(20);
        canvas.drawText("FlexMeet - Meeting Notes", 40, 50, textPaint);

        textPaint.setFakeBoldText(false);
        textPaint.setTextSize(12);
        textPaint.setColor(Color.GRAY);
        canvas.drawText("Meeting ID: " + meetingId, 40, 75, textPaint);
        canvas.drawText("Exported on: " + DateFormat.getDateTimeInstance().format(new Date()), 40, 95, textPaint);

        Paint linePaint = new Paint();
        linePaint.setColor(Color.LTGRAY);
        linePaint.setStrokeWidth(1);
        canvas.drawLine(40, 110, 555, 110, linePaint);

        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(14);
        
        canvas.save();
        canvas.translate(40, 130);
        
        StaticLayout staticLayout = StaticLayout.Builder.obtain(content, 0, content.length(), textPaint, 515)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0, 1.2f)
                .setIncludePad(true)
                .build();
        
        staticLayout.draw(canvas);
        canvas.restore();

        document.finishPage(page);

        String fileName = "FlexMeet_Notes_" + meetingId + "_" + System.currentTimeMillis() + ".pdf";
        
        try {
            OutputStream fos;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/FlexMeet");
                
                Uri uri = requireContext().getContentResolver().insert(MediaStore.Files.getContentUri("external"), contentValues);
                fos = requireContext().getContentResolver().openOutputStream(uri);
            } else {
                File docDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "FlexMeet");
                if (!docDir.exists()) docDir.mkdirs();
                File file = new File(docDir, fileName);
                fos = new FileOutputStream(file);
            }

            if (fos != null) {
                document.writeTo(fos);
                fos.close();
                Toast.makeText(getContext(), "PDF saved to Documents/FlexMeet", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            Toast.makeText(getContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            document.close();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
