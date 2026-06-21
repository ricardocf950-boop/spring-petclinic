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
/**
	 * REFACTORED ORIGINAL METHODS
	 * Cleaned up the data binding process to ensure that models are loaded independently,
	 * preventing unintended side effects or duplicate rows during binding.
	 */
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

	// Creation Flow - Get: Instantiates a clean, isolated object for the view
	@GetMapping("/owners/{ownerId}/pets/{petId}/visits/new")
	public String initNewVisitForm(@ModelAttribute("pet") Pet pet, Map<String, Object> model) {
		Visit visit = new Visit();
		model.put("visit", visit);
		return "pets/createOrUpdateVisitForm";
	}

	// Creation Flow - Post: Adds the visit to the pet list and persists upon confirmation
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
	 * Explicitly looks up and binds the specific visit instance by its numeric ID
	 * to prevent cross-mutations inside the collection.
	 */
	@GetMapping("/owners/{ownerId}/pets/{petId}/visits/{visitId}/edit")
	public String initEditVisitForm(@ModelAttribute("pet") Pet pet, @PathVariable("visitId") int visitId, Map<String, Object> model) {
		if (pet != null) {
			for (Visit v : pet.getVisits()) {
				// Checks if the database ID matches the URL parameter exactly
				if (v.getId() != null && v.getId().equals(visitId)) {
					model.put("visit", v);
					return "pets/createOrUpdateVisitForm";
				}
			}
		}
		return "redirect:/owners/{ownerId}";
	}

	@PostMapping("/owners/{ownerId}/pets/{petId}/visits/{visitId}/edit")
	public String processEditVisitForm(@ModelAttribute("owner") Owner owner, @PathVariable("petId") int petId, @PathVariable("visitId") int visitId, 
			@Valid @ModelAttribute("visit") Visit visit, BindingResult result, RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			return "pets/createOrUpdateVisitForm";
		}

		Pet pet = owner.getPet(petId);
		if (pet != null) {
			for (Visit v : pet.getVisits()) {
				if (v.getId() != null && v.getId().equals(visitId)) {
					// Mutates ONLY the specific targeted row matching the database primary key
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
