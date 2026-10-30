import { useQuery } from '@tanstack/react-query'
import {
  getAnimal,
  getAnimalTreatments,
  getTreatmentEligibility,
} from '../api/animals'

export function useAnimal(animalId) {
  const enabled = Boolean(animalId)

  const animal = useQuery({
    queryKey: ['animal', animalId],
    queryFn: () => getAnimal(animalId),
    enabled,
  })

  const treatments = useQuery({
    queryKey: ['animal', animalId, 'treatments'],
    queryFn: () => getAnimalTreatments(animalId),
    enabled,
  })

  const eligibility = useQuery({
    queryKey: ['animal', animalId, 'eligibility'],
    queryFn: () => getTreatmentEligibility(animalId),
    enabled,
  })

  return { animal, treatments, eligibility }
}
