package com.resolum.alivia.features.nursing.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resolum.alivia.core.ui.components.AliviaCard
import com.resolum.alivia.core.ui.components.Pill
import com.resolum.alivia.core.ui.components.SectionLabel
import com.resolum.alivia.core.ui.theme.Ink
import com.resolum.alivia.core.ui.theme.InkMuted
import com.resolum.alivia.core.ui.theme.Navy
import com.resolum.alivia.core.ui.theme.NavySoft
import com.resolum.alivia.core.ui.theme.Outline
import com.resolum.alivia.core.ui.theme.Surface
import com.resolum.alivia.core.ui.theme.SurfaceMuted
import com.resolum.alivia.core.ui.theme.TealDark
import com.resolum.alivia.core.ui.theme.TealSoft
import com.resolum.alivia.features.nursing.domain.models.AssistedPerson
import com.resolum.alivia.features.nursing.domain.models.CareNetworkMember
import com.resolum.alivia.features.nursing.domain.models.CareRole
import com.resolum.alivia.features.profiles.domain.models.Household
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class HomeSheet { NETWORK, PERSON }

private fun initialsOf(name: String) = name.split(" ").filter { it.isNotBlank() }.mapNotNull { it.firstOrNull() }.take(2)
    .joinToString("").uppercase()

/** The principal relative's name; the backend keeps it as the assisted person's emergency contact. */
private fun principalName(person: AssistedPerson?) = person?.emergencyContactName

@Composable
private fun InitialsAvatar(text: String, size: Int, container: Color = NavySoft, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size.dp).clip(CircleShape).background(container).border(2.dp, Surface, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (text.isEmpty()) {
            Icon(Icons.Outlined.Person, contentDescription = null, tint = Navy, modifier = Modifier.size((size / 2).dp))
        } else {
            Text(text, color = Navy, fontWeight = FontWeight.Bold, fontSize = (size / 2.8).sp)
        }
    }
}

@Composable
private fun CardHeader(title: String, trailing: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(NavySoft), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Groups, contentDescription = null, tint = Navy, modifier = Modifier.size(18.dp))
        }
        Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = Ink, modifier = Modifier.padding(start = 10.dp))
        Spacer(Modifier.weight(1f))
        trailing?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Ink) }
    }
}

@Composable
private fun CardLink(text: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onClick) {
            Text(text, style = MaterialTheme.typography.labelLarge, color = Navy)
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Navy, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun CareNetworkCard(members: List<CareNetworkMember>, person: AssistedPerson?, onOpen: () -> Unit) {
    AliviaCard {
        CardHeader("Red de cuidado", if (members.isEmpty()) null else "${members.size} vinculados")
        if (members.isEmpty()) {
            Text("Todavía no hay otras personas en la red de cuidado.", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val principal = principalName(person)
                Box(Modifier.width((40 + 28 * (minOf(members.size, 3) - 1)).dp)) {
                    members.take(3).forEachIndexed { index, member ->
                        val text = when {
                            index == 2 && members.size > 3 -> "+${members.size - 2}"
                            member.role == CareRole.PRINCIPAL && principal != null -> initialsOf(principal)
                            else -> ""
                        }
                        InitialsAvatar(text, 40, if (index == 0) TealSoft else NavySoft, Modifier.offset(x = (28 * index).dp))
                    }
                }
                Column(Modifier.padding(start = 14.dp)) {
                    Text(principal ?: "Familiar principal", style = MaterialTheme.typography.titleSmall, color = Navy)
                    Text("Principal (familiar)", style = MaterialTheme.typography.bodySmall, color = TealDark)
                }
            }
        }
        CardLink("Ver red de cuidado", onOpen)
    }
}

@Composable
fun AssistedPersonCard(person: AssistedPerson?, household: Household?, today: LocalDate, onOpen: () -> Unit) {
    AliviaCard {
        if (person == null) {
            CardHeader("Persona asistida", null)
            Text("Todavía no tienes una persona asistida asignada.", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
            return@AliviaCard
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            InitialsAvatar(person.initials, 64, TealSoft)
            Column(Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(person.fullName, style = MaterialTheme.typography.titleLarge, color = Navy)
                Pill("Sin alertas", NavySoft, Navy, dot = TealDark)
            }
            person.age(today)?.let { Text("$it años", style = MaterialTheme.typography.bodySmall, color = Ink) }
        }
        HorizontalDivider(color = Outline)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(household?.alias ?: "Asistencia continua", style = MaterialTheme.typography.bodySmall, color = Ink,
                modifier = Modifier.weight(1f))
            TextButton(onClick = onOpen) {
                Text("Ver perfil", style = MaterialTheme.typography.labelLarge, color = Navy)
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Navy, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareNetworkSheet(members: List<CareNetworkMember>, person: AssistedPerson?, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Red de cuidado", style = MaterialTheme.typography.headlineSmall, color = Navy)
            person?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InitialsAvatar(it.initials, 40, TealSoft)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(it.fullName, style = MaterialTheme.typography.titleSmall, color = Navy)
                        Text("Persona asistida", style = MaterialTheme.typography.bodySmall, color = InkMuted)
                    }
                    Pill("${members.size} vinculados", NavySoft, Navy)
                }
            }
            if (members.isEmpty()) {
                Text("Todavía no hay otras personas en la red de cuidado.", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
            }
            members.groupBy { it.role }.forEach { (role, group) ->
                SectionLabel(when (role) {
                    CareRole.PRINCIPAL -> "Cuidador principal"
                    CareRole.CAREGIVER -> "Cuidadores"
                    CareRole.GUEST -> "Invitados"
                }, Modifier.padding(top = 4.dp))
                AliviaCard {
                    group.forEachIndexed { index, _ ->
                        if (index > 0) HorizontalDivider(color = Outline)
                        val name = if (role == CareRole.PRINCIPAL) principalName(person) else null
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            InitialsAvatar(name?.let(::initialsOf).orEmpty(), 36)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(name ?: "${role.label} ${index + 1}", style = MaterialTheme.typography.titleSmall, color = Navy)
                                Text(if (role == CareRole.PRINCIPAL) "Familiar responsable" else role.label,
                                    style = MaterialTheme.typography.bodySmall, color = InkMuted)
                            }
                            Text("Vinculado", style = MaterialTheme.typography.labelMedium, color = TealDark)
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).background(SurfaceMuted)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = Navy, modifier = Modifier.size(18.dp))
                Text("La gestión de cuidadores se realiza desde la plataforma web.", style = MaterialTheme.typography.bodySmall, color = Ink)
            }
        }
    }
}

private val birthFormat = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-PE"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistedPersonSheet(person: AssistedPerson, household: Household?, today: LocalDate, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            InitialsAvatar(person.initials, 88, TealSoft)
            Text(person.fullName, style = MaterialTheme.typography.headlineSmall, color = Navy, textAlign = TextAlign.Center)
            Pill("Perfil completo", TealSoft, TealDark, dot = TealDark)
            val age = person.age(today)
            Text(listOfNotNull(age?.let { "$it años" }, household?.alias).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = InkMuted)
            SectionLabel("Información personal", Modifier.fillMaxWidth().padding(top = 8.dp))
            AliviaCard {
                InfoRow("Nombres", person.firstName)
                InfoRow("Apellidos", person.lastName)
                person.dni?.let { InfoRow("Documento", "DNI $it") }
                person.birthDate?.let { InfoRow("Fecha de nacimiento", it.format(birthFormat) + (age?.let { a -> " ($a años)" } ?: "")) }
                person.kinshipLabel?.let { InfoRow("Parentesco", it) }
                person.mobilityLabel?.let { InfoRow("Movilidad", it) }
            }
            SectionLabel("Información asociada", Modifier.fillMaxWidth().padding(top = 8.dp))
            AliviaCard {
                household?.let { InfoRow("Hogar relacionado", it.alias) }
                person.emergencyContactName?.let { InfoRow("Cuidador principal", it) }
                person.emergencyContactPhone?.let { InfoRow("Teléfono de contacto", "+$it") }
                person.phoneNumber?.let { InfoRow("Teléfono de la persona", "+$it") }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = InkMuted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall, color = Ink, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
    }
}
