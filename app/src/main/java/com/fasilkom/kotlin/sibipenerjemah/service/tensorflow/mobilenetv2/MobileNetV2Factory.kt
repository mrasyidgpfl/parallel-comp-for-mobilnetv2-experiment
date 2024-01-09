package id.ac.ui.cs.skripsi.misael_jonathan.sibikotlin.Tensorflow.MOBILENETV2

import android.content.res.AssetManager
import com.fasilkom.kotlin.sibipenerjemah.service.tensorflow.utils.FileUtils
import org.tensorflow.contrib.android.TensorFlowInferenceInterface
import java.io.IOException
import java.util.*

object MobileNetV2Factory {

    enum class ModelType() {
        REGULER, MAJORITY, INPUT_NORM, WITH_NORM
    }

    /* Directory of models (next will use internal memory) */
    private const val MODEL_MOBILENETV2_DIR = "protobuf/Weight_MobileNet_V2_20191020_054829_K_1-5_Epoch_50.pb"
    private const val MODEL_MOBILENETV2_PIPUT_DIR = "protobufv2/Weight_MobileNet_V2_20191020_054829_K_1-5_Epoch_50.pb"
    private const val MODEL_MOBILENETV2_INPUT_NORMALIZED = "protobuftest/Weight_MobileNet_V2_20200924_041343_K_1-5_Epoch_1_input_normalize.pb"
    private const val MODEL_MOBILENETV2_WITH_NORMALIZE = "protobuftest/Weight_MobileNet_V2_20200924_092450_K_2-5_Epoch_5_with_normalize.pb"

    /* Const */
    private const val INPUT_NAME = "input_1"
    private const val OUTPUT_NAME = "flatten_1/Reshape"
    private const val LABEL_DIR = "labels.txt"
    private const val INPUT_SIZE = 224
    private const val NUM_OF_CHANNEL = 3
    private const val NUM_OF_CLASSES = 1280
    private const val NUM_OF_INFERENCES = 4

    @Throws(IOException::class)
    fun create(
        assetManager: AssetManager,
        type: ModelType = ModelType.MAJORITY
    ): MobileNetV2Classifier? {

        var chosenModel = when (type) {
            ModelType.REGULER -> MODEL_MOBILENETV2_DIR
            ModelType.MAJORITY -> MODEL_MOBILENETV2_PIPUT_DIR
            ModelType.INPUT_NORM -> MODEL_MOBILENETV2_INPUT_NORMALIZED
            ModelType.WITH_NORM -> MODEL_MOBILENETV2_WITH_NORMALIZE
        }

        val labels: List<String> =
            FileUtils.getLabels(assetManager, LABEL_DIR)
        val tensorFlowInferenceInterfaces =
            ArrayList<TensorFlowInferenceInterface>()
        for (i in 0 until NUM_OF_INFERENCES) {
            tensorFlowInferenceInterfaces.add(
                TensorFlowInferenceInterface(
                    assetManager,
                    chosenModel
                )
            )
        }
        return MobileNetV2Classifier(
            INPUT_NAME,
            OUTPUT_NAME,
            INPUT_SIZE,
            NUM_OF_CHANNEL,
            NUM_OF_CLASSES,
            labels,
            tensorFlowInferenceInterfaces
        )
    }

}
