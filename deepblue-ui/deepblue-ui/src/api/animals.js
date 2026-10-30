import { request } from './client'

export const getAnimal = (animalId) =>
  request(`/api/animals/${animalId}`)

export const getAnimalTreatments = (animalId) =>
  request(`/api/animals/${animalId}/treatments`)

export const getTreatmentEligibility = (animalId) =>
  request(`/api/animals/${animalId}/treatment-eligibility`)
