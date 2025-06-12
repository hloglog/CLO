package com.example.clo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import android.app.Activity

class ClosetFragment : Fragment() {
    private lateinit var topButton: MaterialButton
    private lateinit var bottomButton: MaterialButton
    private lateinit var shoesButton: MaterialButton
    private lateinit var accessoriesButton: MaterialButton
    private lateinit var clothesRecyclerView: RecyclerView
    private var currentPhotoPath: String? = null
    private var isFragmentActive = true
    
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Handle the camera photo
            currentPhotoPath?.let { path ->
                // Process the captured image
                Toast.makeText(requireContext(), "이미지가 저장되었습니다: $path", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // Handle the selected image
                Toast.makeText(requireContext(), "이미지가 선택되었습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    companion object {
        private const val PERMISSIONS_REQUEST = 100
        private const val TAG = "ClosetFragment"
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_closet, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            initializeViews(view)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onViewCreated", e)
            Toast.makeText(requireContext(), "앱 초기화 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeViews(view: View) {
        // Initialize views
        topButton = view.findViewById(R.id.topButton)
        bottomButton = view.findViewById(R.id.bottomButton)
        shoesButton = view.findViewById(R.id.shoesButton)
        accessoriesButton = view.findViewById(R.id.accessoriesButton)
        clothesRecyclerView = view.findViewById(R.id.clothesRecyclerView)

        // Set up category buttons
        setupCategoryButtons()

        // Initialize RecyclerView
        clothesRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        // TODO: Set adapter when you have data to display

        // Set up add button click listener
        view.findViewById<FloatingActionButton>(R.id.addButton).setOnClickListener {
            if (isFragmentActive) {
                showImageSourceDialog()
            }
        }
    }

    private fun setupCategoryButtons() {
        // 초기 상태: 상의 선택
        selectCategory("상의")
        
        topButton.setOnClickListener { selectCategory("상의") }
        bottomButton.setOnClickListener { selectCategory("하의") }
        shoesButton.setOnClickListener { selectCategory("신발") }
        accessoriesButton.setOnClickListener { selectCategory("기타") }
    }

    private fun selectCategory(category: String) {
        // 모든 버튼을 기본 스타일로 초기화
        resetAllButtons()
        
        when (category) {
            "상의" -> {
                topButton.setBackgroundColor(resources.getColor(R.color.black, requireContext().theme))
                topButton.setTextColor(resources.getColor(R.color.white, requireContext().theme))
            }
            "하의" -> {
                bottomButton.setBackgroundColor(resources.getColor(R.color.black, requireContext().theme))
                bottomButton.setTextColor(resources.getColor(R.color.white, requireContext().theme))
            }
            "신발" -> {
                shoesButton.setBackgroundColor(resources.getColor(R.color.black, requireContext().theme))
                shoesButton.setTextColor(resources.getColor(R.color.white, requireContext().theme))
            }
            "기타" -> {
                accessoriesButton.setBackgroundColor(resources.getColor(R.color.black, requireContext().theme))
                accessoriesButton.setTextColor(resources.getColor(R.color.white, requireContext().theme))
            }
        }
    }

    private fun resetAllButtons() {
        val buttons = listOf(topButton, bottomButton, shoesButton, accessoriesButton)
        buttons.forEach { button ->
            button.setBackgroundColor(resources.getColor(R.color.white, requireContext().theme))
            button.setTextColor(resources.getColor(R.color.black, requireContext().theme))
        }
    }

    override fun onResume() {
        super.onResume()
        isFragmentActive = true
    }

    override fun onPause() {
        super.onPause()
        isFragmentActive = false
    }

    override fun onDestroy() {
        super.onDestroy()
        isFragmentActive = false
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("카메라로 촬영", "갤러리에서 선택")
        AlertDialog.Builder(requireContext())
            .setTitle("이미지 선택")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndLaunch()
                    1 -> checkGalleryPermissionAndLaunch()
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), PERMISSIONS_REQUEST)
        } else {
            launchCamera()
        }
    }

    private fun checkGalleryPermissionAndLaunch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSIONS_REQUEST)
            } else {
                launchGallery()
            }
        } else {
            launchGallery()
        }
    }

    private fun launchCamera() {
        val photoFile = try {
            createImageFile()
        } catch (ex: IOException) {
            Toast.makeText(requireContext(), "이미지 파일 생성 실패", Toast.LENGTH_SHORT).show()
            return
        }

        currentPhotoPath = photoFile.absolutePath
        val photoURI = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", photoFile)
        
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        }
        
        takePictureLauncher.launch(takePictureIntent)
    }

    private fun launchGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, you can now access camera/gallery
                    Toast.makeText(requireContext(), "권한이 허용되었습니다", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "권한이 거부되었습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
} 