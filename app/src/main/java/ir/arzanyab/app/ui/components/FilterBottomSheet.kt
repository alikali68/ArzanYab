package ir.arzanyab.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.arzanyab.app.domain.model.SearchFilterState
import ir.arzanyab.app.domain.model.SortOption
import ir.arzanyab.app.util.PersianFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    filterState: SearchFilterState,
    availableSources: List<Pair<String, String>>,
    onApplyFilters: (SearchFilterState) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedSort by remember { mutableStateOf(filterState.sortOption) }
    var minTrust by remember { mutableFloatStateOf(filterState.minTrustScore.toFloat()) }
    var selectedSources by remember { mutableStateOf(filterState.selectedSources) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(text = "مرتب‌سازی", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SortOption.entries.forEach { option ->
                    FilterChip(
                        selected = selectedSort == option,
                        onClick = { selectedSort = option },
                        label = { Text(option.title) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "منابع قیمت", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                availableSources.forEach { (id, title) ->
                    val isSelected = id in selectedSources
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedSources = if (isSelected) {
                                if (selectedSources.size > 1) selectedSources - id else selectedSources
                            } else selectedSources + id
                        },
                        label = { Text(title) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "حداقل امتیاز اعتماد: ${PersianFormatter.toPersianDigits(minTrust.toInt().toLong())}٪",
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = minTrust,
                onValueChange = { minTrust = it },
                valueRange = 0f..80f,
                steps = 7
            )

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    onApplyFilters(
                        filterState.copy(
                            sortOption = selectedSort,
                            minTrustScore = minTrust.toInt(),
                            selectedSources = selectedSources
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("اعمال فیلترها")
            }
        }
    }
}
