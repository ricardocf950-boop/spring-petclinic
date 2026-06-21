/*
 * Copyright 2012-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.owner;

import java.util.Map;
import java.util.Optional;
import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.Valid;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 * @author Dave Syer
 * @author Ricardo Costa Filho
 */
@Controller
class VisitController {

	private final OwnerRepository owners;
	
	// 1. INJECTED VISITREPOSITORY FOR ISOLATED DATABASE UPDATES
	private final VisitRepository visits;

	public VisitController(OwnerRepository owners, VisitRepository visits) {
		this.owners = owners;
		this.visits = visits;
	}

	@InitBinder
	public void setAllowedFields(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id", "*.id");
	}

	@ModelAttribute("owner")
	public Owner loadOwner(@PathVariable("ownerId") int ownerId) {
		Optional<Owner> optionalOwner = this.owners.findById(ownerId);
		return optionalOwner.orElseThrow(() -> new IllegalArgumentException(
				"Owner not found with id: " + ownerId + ". Please ensure the ID is correct"));
	}

	@ModelAttribute("pet")
	public Pet loadPet(@PathVariable("ownerId") int ownerId, @PathVariable("petId") int petId, Map<String, Object> model) {
		Owner owner = (Owner) model.get("owner");
		if (owner == null) {
			Optional<Owner> optionalOwner = this.owners.findById(ownerId);
			owner = optionalOwner.orElseThrow(() -> new IllegalArgumentException("Owner not found"));
			model.put("owner", owner);
		}
		Pet pet = owner.getPet(petId);
		if (pet == null) {
			throw new IllegalArgumentException("Pet with id " + petId + " not found.");
		}
		return pet;
	}

	@GetMapping("/owners/{ownerId}/pets/{petId}/visits/new")
	public String initNewVisitForm(@ModelAttribute("pet") Pet pet, Map<String, Object> model) {
		Visit visit = new Visit();
		model.put("visit", visit);
		return "pets/createOrUpdateVisitForm";
	}

	@PostMapping("/owners/{ownerId}/pets/{petId}/visits/new")
	public String processNewVisitForm(@ModelAttribute("owner") Owner owner, @PathVariable int petId, 
			@Valid @ModelAttribute("visit") Visit visit, BindingResult result, RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			return "pets/createOrUpdateVisitForm";
		}

		owner.addVisit(petId, visit);
		this.owners.save(owner);
		redirectAttributes.addFlashAttribute("message", "Your visit has been booked");
		return "redirect:/owners/{ownerId}";
	}

	/**
	 * EDIT FLOW METHODS (ISSUE #2338)
	 * Fetches the object directly from the database by its unique id, bypassing parent graph issues.
	 */
	@GetMapping("/owners/{ownerId}/pets/{petId}/visits/{visitId}/edit")
	public String initEditVisitForm(@PathVariable("visitId") int visitId, Map<String, Object> model) {
		// Fetches single row using the specific VisitRepository mapping
		Optional<Visit> optionalVisit = this.visits.findById(visitId);
		if (optionalVisit.isPresent()) {
			model.put("visit", optionalVisit.get());
			return "pets/createOrUpdateVisitForm";
		}
		return "redirect:/owners/{ownerId}";
	}

	@PostMapping("/owners/{ownerId}/pets/{petId}/visits/{visitId}/edit")
	public String processEditVisitForm(
			@PathVariable("ownerId") int ownerId,
			@PathVariable("petId") int petId,
			@PathVariable("visitId") int visitId, 
			@RequestParam("description") String description,
			@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			RedirectAttributes redirectAttributes) {
		
		if (date.isBefore(LocalDate.now())) {
			redirectAttributes.addFlashAttribute("error", "The visit date must be today or a future date.");
			return "redirect:/owners/" + ownerId + "/pets/" + petId + "/visits/" + visitId + "/edit";
		}

		Optional<Visit> optionalVisit = this.visits.findById(visitId);
		if (optionalVisit.isPresent()) {
			Visit existingVisit = optionalVisit.get();
			
			// Updates only the single targeted database record by its unique primary key ID
			existingVisit.setDescription(description);
			existingVisit.setDate(date);
			
			this.visits.save(existingVisit); 
			redirectAttributes.addFlashAttribute("message", "The visit description has been successfully updated");
		}
		return "redirect:/owners/" + ownerId;
	}

}
