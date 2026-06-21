/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap; // Adicionado para suporte na edição
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 * @author Dave Syer
 * @author Wick Dynex
 */
@Controller
class VisitController {

	private final OwnerRepository owners;

	public VisitController(OwnerRepository owners) {
		this.owners = owners;
	}

	@InitBinder
	public void setAllowedFields(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id", "*.id");
	}

	/**
	 * Called before each and every @RequestMapping annotated method. 2 goals: - Make sure
	 * we always have fresh data - Since we do not use the session scope, make sure that
	 * Pet object always has an id (Even though id is not part of the form fields)
	 * @param petId
	 * @return Pet
	 */
	@ModelAttribute("visit")
	public Visit loadPetWithVisit(@PathVariable("ownerId") int ownerId, @PathVariable("petId") int petId,
			Map<String, Object> model, HttpServletRequest request) {
		Optional<Owner> optionalOwner = owners.findById(ownerId);
		Owner owner = optionalOwner.orElseThrow(() -> new IllegalArgumentException(
				"Owner not found with id: " + ownerId + ". Please ensure the ID is correct "));

		Pet pet = owner.getPet(petId);
		if (pet == null) {
			throw new IllegalArgumentException(
					"Pet with id " + petId + " not found for owner with id " + ownerId + ".");
		}
		model.put("pet", pet);
		model.put("owner", owner);

		if (request.getRequestURI().endsWith("/new")) {
			Visit visit = new Visit();
			pet.addVisit(visit);
			return visit;
		}
		
		return new Visit();
	}

	// Spring MVC calls method loadPetWithVisit(...) before initNewVisitForm is
	// called
	@GetMapping("/owners/{ownerId}/pets/{petId}/visits/{visitId}/edit")
	public String initEditVisitForm(@ModelAttribute Owner owner, @PathVariable int petId, @PathVariable int visitId, ModelMap model) {
		Pet pet = owner.getPet(petId);
		if (pet != null) {
			for (Visit v : pet.getVisits()) {
				if (v.getId() != null && v.getId() == visitId) {
					model.put("visit", v);
					return "pets/createOrUpdateVisitForm";
				}
			}
		}
		return "redirect:/owners/{ownerId}";
	}

	@PostMapping("/owners/{ownerId}/pets/{petId}/visits/{visitId}/edit")
	public String processEditVisitForm(@ModelAttribute Owner owner, @PathVariable int petId, @PathVariable int visitId, 
			@Valid @ModelAttribute("visit") Visit visit, BindingResult result, RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			return "pets/createOrUpdateVisitForm";
		}

		Pet pet = owner.getPet(petId);
		if (pet != null) {
			for (Visit v : pet.getVisits()) {
				if (v.getId() != null && v.getId() == visitId) {
					v.setDescription(visit.getDescription());
					v.setDate(visit.getDate());
					break;
				}
			}
			this.owners.save(owner);
			redirectAttributes.addFlashAttribute("message", "The visit description has been successfully updated");
		}
		return "redirect:/owners/{ownerId}";
	}
	/**
	 * NEW: Method to initialize the form for editing an existing visit description.
	 * Part of Issue #2338.
	 */
	@GetMapping("/owners/{ownerId}/pets/{petId}/visits/{visitId}/edit")
	public String initEditVisitForm(@ModelAttribute Owner owner, @PathVariable int petId, @PathVariable int visitId, ModelMap model) {
		Pet pet = owner.getPet(petId);
		if (pet != null) {
			for (Visit v : pet.getVisits()) {
				if (v.getId() != null && v.getId() == visitId) {
					model.put("visit", v);
					break;
				}
			}
		}
		return "pets/createOrUpdateVisitForm"; // Reutilizando a view existente
	}

	/**
	 * NEW: Method to process the modification of an existing visit description.
	 * Part of Issue #2338.
	 */
	@PostMapping("/owners/{ownerId}/pets/{petId}/visits/{visitId}/edit")
	public String processEditVisitForm(@ModelAttribute Owner owner, @PathVariable int petId, @PathVariable int visitId, 
			@Valid Visit visit, BindingResult result, RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			return "pets/createOrUpdateVisitForm";
		}

		Pet pet = owner.getPet(petId);
		if (pet != null) {
			for (Visit v : pet.getVisits()) {
				if (v.getId() != null && v.getId() == visitId) {
					v.setDescription(visit.getDescription());
					v.setDate(visit.getDate());
					break;
				}
			}
			this.owners.save(owner);
			redirectAttributes.addFlashAttribute("message", "The visit description has been successfully updated");
		}
		return "redirect:/owners/{ownerId}";
	}

}
